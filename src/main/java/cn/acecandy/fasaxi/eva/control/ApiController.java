package cn.acecandy.fasaxi.eva.control;

import cn.acecandy.fasaxi.eva.common.resp.Rsres;
import cn.acecandy.fasaxi.eva.task.impl.PowerRankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * api控制器
 *
 * @author AceCandy
 * @since 2025/05/27
 */
@Tag(name = "api")
@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private PowerRankService powerRankService;

    @Operation(summary = "当前系统时间")
    @GetMapping("powerRankCheck")
    public Rsres<Object> powerRankCheck() {
        powerRankService.powerRankCheck(2);
        return Rsres.success(true);
    }

}