package com.boylu.controller.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.common.Result;
import com.boylu.entity.SysMessage;
import com.boylu.service.SysMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: boylu
 * @date: 2025/1/2
 * @description:
 */
@RestController
@Api(tags = "留言管理")
@RequestMapping("/sys/message")
@RequiredArgsConstructor
public class SysMessageController {

    private final SysMessageService sysMessageService;

    @GetMapping("/list")
    @ApiOperation(value = "获取留言列表")
    public Result<Page<SysMessage>> list() {
        return Result.success(sysMessageService.selectList());
    }

    @DeleteMapping("/delete/{ids}")
    @ApiOperation(value = "删除留言")
    @SaCheckPermission("sys:message:delete")
    public Result<Void> delete(@PathVariable List<Integer> ids) {
        sysMessageService.removeBatchByIds(ids);
        return Result.success();
    }
}
