package com.boylu.controller.resource;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boylu.common.Result;
import com.boylu.entity.SysResource;
import com.boylu.service.ResourceService;
import com.boylu.vo.resource.SysResourceVo;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Api(tags = "门户-资源管理")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/list")
    @Operation(description = "资源列表")
    public Result<Page<SysResourceVo>> getResourceList(SysResource sysResource) {
        return Result.success(resourceService.getResourceList(sysResource));
    }

    @SaCheckLogin
    @PostMapping("/add")
    @Operation(description = "上传资源")
    public Result<Void> add(@RequestBody SysResource sysResource) {
        resourceService.add(sysResource);
        return Result.success();
    }

    @GetMapping("/verify")
    @Operation(description = "校验验证码")
    @SaCheckLogin
    public Result<SysResource> verify(String code,Long id) {
        return Result.success(resourceService.verify(code,id));
    }

    @GetMapping("/download")
    @Operation(description = "获取免费资源下载链接")
    @SaCheckLogin
    public Result<SysResource> download(Long id) {
        return Result.success(resourceService.download(id));
    }

}
