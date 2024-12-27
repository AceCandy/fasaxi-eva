package cn.acecandy.fasaxi.eva.control;

import cn.acecandy.fasaxi.eva.bean.req.VideoRedirectReq;
import cn.acecandy.fasaxi.eva.service.FastEmbyService;
import cn.acecandy.fasaxi.eva.utils.EmbyUtil;
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
    @GetMapping("emby/videos/{videoId}/*")
    public ResponseEntity<?> handleEmbyRequest(@PathVariable String videoId, HttpServletRequest request,
                                               VideoRedirectReq req) {
        log.info("收到请求: [{}]{}, 提取的参数:{}", request.getMethod(),
                request.getRequestURI(), request.getParameterMap());
        return fastEmbyService.handleEmbyRequest(EmbyUtil.parseHttpReq(request), videoId, req);
    }

}