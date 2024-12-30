package cn.acecandy.fasaxi.eva.sql.mapper;

import cn.acecandy.fasaxi.eva.sql.entity.WodiWord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 卧底词汇 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Mapper
public interface WodiWordMapper extends BaseMapper<WodiWord> {

    @Select("SELECT * FROM wodi_word ORDER BY RAND() / (play_time + 1) LIMIT 10")
    List<WodiWord> selectRandomWord10();
}