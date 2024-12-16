package cn.acecandy.fasaxi.eva.control;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Emby处理-模仿fastEmby")
@RestController
@RequestMapping("/emby")
public class FastEmbyController {

    @Resource
    private FastEmbyService fastEmbyService;

    @Operation(summary = "emby请求")
    @GetMapping("/{path:path}")
    public ResponseEntity<?> handleEmbyRequest(@PathVariable String path, HttpServletRequest request) {
        return fastEmbyService.handleEmbyRequest(path, request);
    }

    @Operation(summary = "重定向请求")
    @GetMapping("/Videos/{path:path}")
    public ResponseEntity<?> handleVideosRequest(@PathVariable String path, HttpServletRequest request) {
        return fastEmbyService.handleVideosRequest(path, request);
    }

}