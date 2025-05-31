package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 续命码
 *
 * @author AceCandy
 * @since 2025/05/30
 */
@TableName(value = "x_renew")
@Data
public class XRenew {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Dmail码子
     */
    private String code;

    /**
     * 使用人tgId
     */
    private Long tgId;

    /**
     * 币值
     */
    private Integer iv;

    /**
     * 是否使用 0-未使用 1-已使用
     */
    private Boolean isUse;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;
}