package com.boylu.controller.message;

import com.boylu.annotation.AccessLimit;
import com.boylu.service.MessageService;
import com.boylu.common.Result;
import com.boylu.entity.SysMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
@Api(tags = "门户-留言管理")
public class MessageController {

    private final MessageService messageService;

    @AccessLimit
    @GetMapping("/list")
    @ApiOperation(value = "留言列表")
    public Result<List<SysMessage>> getMessageList() {
        return Result.success(messageService.getMessageList());
    }

    @PostMapping("/add")
    @ApiOperation(value = "发表留言")
    public Result<Boolean> add(@RequestBody SysMessage sysMessage) {
        return Result.success(messageService.add(sysMessage));
    }
}
