#!/usr/bin/env python3
import argparse
import hashlib
import os
import shutil
import subprocess
from collections import defaultdict
from datetime import datetime
from pathlib import Path


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif", ".bmp"}
TEXT_COLUMN_TYPES = {
    "char",
    "varchar",
    "tinytext",
    "text",
    "mediumtext",
    "longtext",
    "json",
}


def mysql_query(args, sql):
    env = os.environ.copy()
    if args.db_password:
        env["MYSQL_PWD"] = args.db_password
    command = [
        "mysql",
        "-N",
        "-B",
        "-h",
        args.db_host,
        "-u",
        args.db_user,
        args.db_name,
        "-e",
        sql,
    ]
    return subprocess.check_output(command, env=env, text=True, errors="ignore")


def quote_identifier(value):
    return "`" + value.replace("`", "``") + "`"


def load_reference_text(args):
    columns_sql = """
        SELECT TABLE_NAME, COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND DATA_TYPE IN ('char','varchar','tinytext','text','mediumtext','longtext','json')
          {table_filter}
        ORDER BY TABLE_NAME, ORDINAL_POSITION
    """.format(
        table_filter=(
            ""
            if args.reference_mode == "all"
            else "AND TABLE_NAME NOT IN ('file_detail','file_part_detail','sys_file_oss')"
        )
    )
    columns_output = mysql_query(args, columns_sql)
    chunks = []
    for line in columns_output.splitlines():
        parts = line.split("\t")
        if len(parts) != 2:
            continue
        table, column = parts
        sql = (
            "SELECT {column} FROM {table} "
            "WHERE {column} IS NOT NULL AND CAST({column} AS CHAR) <> ''"
        ).format(table=quote_identifier(table), column=quote_identifier(column))
        try:
            values = mysql_query(args, sql)
        except subprocess.CalledProcessError:
            continue
        if values:
            chunks.append(values)
    return "\n".join(chunks).replace("\\", "/").lower()


def load_file_detail_records(args):
    sql = """
        SELECT id, url, filename, base_path, path
        FROM file_detail
        WHERE url IS NOT NULL AND url <> ''
    """
    records = []
    try:
        output = mysql_query(args, sql)
    except subprocess.CalledProcessError:
        return records
    for line in output.splitlines():
        parts = line.split("\t")
        if len(parts) < 5:
            continue
        records.append({
            "id": parts[0],
            "url": parts[1].replace("\\", "/"),
            "filename": parts[2],
            "base_path": parts[3].replace("\\", "/"),
            "path": parts[4].replace("\\", "/"),
        })
    return records


def iter_image_files(storage_root):
    for path in storage_root.rglob("*"):
        if not path.is_file():
            continue
        rel_parts = {part.lower() for part in path.relative_to(storage_root).parts}
        if "img-cache" in rel_parts or ".cleanup-quarantine" in rel_parts:
            continue
        if path.suffix.lower() not in IMAGE_EXTENSIONS:
            continue
        yield path


def file_sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def is_referenced(path, storage_root, reference_text):
    rel = path.relative_to(storage_root).as_posix().lower()
    candidates = {
        rel,
        "/" + rel,
        "/localfile/" + rel,
        path.name.lower(),
    }
    return any(candidate in reference_text for candidate in candidates)


def file_detail_ids_for_path(path, storage_root, records):
    rel = path.relative_to(storage_root).as_posix().lower()
    ids = []
    for record in records:
        url = record["url"].lower()
        joined = (record["base_path"] + record["path"] + record["filename"]).lower()
        if rel in url or rel.endswith(joined) or url.endswith("/" + rel):
            ids.append(record["id"])
    return ids


def choose_keep_file(files, storage_root, reference_text):
    referenced = [p for p in files if is_referenced(p, storage_root, reference_text)]
    if referenced:
        return min(referenced, key=lambda p: (len(str(p)), str(p)))
    return min(files, key=lambda p: (p.stat().st_mtime, len(str(p)), str(p)))


def main():
    parser = argparse.ArgumentParser(description="Find and quarantine duplicate image files in blog storage.")
    parser.add_argument("--storage-root", default="/opt/boylu-blog/storage")
    parser.add_argument("--db-host", default="127.0.0.1")
    parser.add_argument("--db-user", default="boylu")
    parser.add_argument("--db-password", default=os.environ.get("DB_PASSWORD", ""))
    parser.add_argument("--db-name", default="blog")
    parser.add_argument("--delete", action="store_true", help="Quarantine safe duplicate files.")
    parser.add_argument("--quarantine-dir", default="")
    parser.add_argument(
        "--reference-mode",
        choices=["all", "content"],
        default="all",
        help="all protects file registry rows too; content protects only real content/config references.",
    )
    parser.add_argument(
        "--prune-file-detail",
        action="store_true",
        help="After quarantining candidates, delete matching file_detail rows that are not content-referenced.",
    )
    args = parser.parse_args()

    storage_root = Path(args.storage_root).resolve()
    if not storage_root.is_dir():
        raise SystemExit(f"storage root does not exist: {storage_root}")

    reference_text = load_reference_text(args)
    file_detail_records = load_file_detail_records(args)
    hash_groups = defaultdict(list)
    total_files = 0
    total_bytes = 0

    for image_path in iter_image_files(storage_root):
        total_files += 1
        total_bytes += image_path.stat().st_size
        hash_groups[file_sha256(image_path)].append(image_path)

    duplicate_groups = {hash_value: files for hash_value, files in hash_groups.items() if len(files) > 1}
    candidates = []

    for hash_value, files in duplicate_groups.items():
        keep_file = choose_keep_file(files, storage_root, reference_text)
        for image_path in files:
            if image_path == keep_file:
                continue
            if is_referenced(image_path, storage_root, reference_text):
                continue
            candidates.append((hash_value, keep_file, image_path))

    print(f"storage_root={storage_root}")
    print(f"reference_mode={args.reference_mode}")
    print(f"image_files={total_files}")
    print(f"image_bytes={total_bytes}")
    print(f"duplicate_hash_groups={len(duplicate_groups)}")
    print(f"safe_duplicate_candidates={len(candidates)}")
    print(f"safe_duplicate_bytes={sum(path.stat().st_size for _, _, path in candidates)}")
    candidate_file_detail_ids = []
    if args.reference_mode == "content":
        for _, _, delete_file in candidates:
            candidate_file_detail_ids.extend(file_detail_ids_for_path(delete_file, storage_root, file_detail_records))
        print(f"candidate_file_detail_rows={len(set(candidate_file_detail_ids))}")

    for hash_value, keep_file, delete_file in candidates[:50]:
        print(
            "candidate\t{hash}\tkeep={keep}\tremove={remove}\tsize={size}".format(
                hash=hash_value[:16],
                keep=keep_file.relative_to(storage_root).as_posix(),
                remove=delete_file.relative_to(storage_root).as_posix(),
                size=delete_file.stat().st_size,
            )
        )
    if len(candidates) > 50:
        print(f"candidate_output_truncated={len(candidates) - 50}")

    if not args.delete or not candidates:
        return

    quarantine_root = Path(args.quarantine_dir) if args.quarantine_dir else storage_root / ".cleanup-quarantine" / datetime.now().strftime("%Y%m%d-%H%M%S")
    quarantine_root.mkdir(parents=True, exist_ok=True)
    moved = 0
    moved_bytes = 0
    moved_paths = []
    for _, _, delete_file in candidates:
        rel = delete_file.relative_to(storage_root)
        target = quarantine_root / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        moved_bytes += delete_file.stat().st_size
        shutil.move(str(delete_file), str(target))
        moved_paths.append(delete_file)
        moved += 1

    print(f"quarantine_dir={quarantine_root}")
    print(f"quarantined_files={moved}")
    print(f"quarantined_bytes={moved_bytes}")

    if args.prune_file_detail and args.reference_mode == "content" and moved_paths:
        ids = []
        for moved_path in moved_paths:
            ids.extend(file_detail_ids_for_path(moved_path, storage_root, file_detail_records))
        ids = sorted(set(ids))
        if ids:
            quoted_ids = ",".join("'" + file_id.replace("'", "''") + "'" for file_id in ids)
            backup_sql = f"SELECT * FROM file_detail WHERE id IN ({quoted_ids})"
            backup_output = mysql_query(args, backup_sql)
            (quarantine_root / "file_detail_deleted_rows.tsv").write_text(backup_output, encoding="utf-8")
            delete_sql = f"DELETE FROM file_detail WHERE id IN ({quoted_ids})"
            mysql_query(args, delete_sql)
        print(f"deleted_file_detail_rows={len(ids)}")


if __name__ == "__main__":
    main()
