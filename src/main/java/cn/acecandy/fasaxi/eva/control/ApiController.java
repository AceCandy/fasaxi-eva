package cn.acecandy.fasaxi.eva.control;

import cn.acecandy.fasaxi.eva.common.resp.Rsres;
import cn.acecandy.fasaxi.eva.task.impl.GameService;
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

    @Resource
    private GameService gameService;

    @Operation(summary = "战力榜单更新")
    @GetMapping("powerRankCheck")
    public Rsres<Object> powerRankCheck() {
        powerRankService.powerRankCheck();
        return Rsres.success(true);
    }

    @Operation(summary = "将本地没有的图片下载完成")
    @GetMapping("ktccyToDb")
    public Rsres<Object> ktccyToDb() {
        gameService.ktccyToDb();
        return Rsres.success(true);
    }

}