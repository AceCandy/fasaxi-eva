package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * emby 积分实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Builder
@Data
public class Emby {

    @TableId
    @Schema(name = "主键 tgId")
    private Long tg;

    @Schema(name = "embyId")
    private String embyid;

    @Schema(name = "姓名")
    private String name;

    @Schema(name = "密码")
    private String pwd;

    @Schema(name = "安全码")
    private String pwd2;

    @Schema(name = "a-白名单 b-正常用户 c-禁用 d-删除账号 e-外门")
    private String lv;

    @Schema(name = "创建时间")
    private Date cr;

    @Schema(name = "到期时间")
    private Date ex;

    @Schema(name = "积分/天数")
    private Integer us;

    @Schema(name = "币")
    private Integer iv;

    @Schema(name = "最近登录emby时间？")
    private Date ch;
}