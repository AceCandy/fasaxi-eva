package cn.acecandy.fasaxi.eva.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * wodi 词汇实体
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Data
public class WodiWord {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(name = "主键")
    private Integer id;

    @Schema(name = "平民词")
    private String peopleWord;

    @Schema(name = "卧底词")
    private String spyWord;

    @Schema(name = "使用次数")
    private Integer playTime;
}