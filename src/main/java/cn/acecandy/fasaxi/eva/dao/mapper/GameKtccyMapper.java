package cn.acecandy.fasaxi.eva.dao.mapper;

import cn.acecandy.fasaxi.eva.dao.entity.GameKtccy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 看图猜成语 mapper
 *
 * @author AceCandy
 * @since 2024/10/29
 */
@Mapper
public interface GameKtccyMapper extends BaseMapper<GameKtccy> {

    @Select("SELECT * FROM game_ktccy where file_url is not null ORDER BY RAND() / (play_time + 1) LIMIT 50")
    List<GameKtccy> selectRandomWord10();
}