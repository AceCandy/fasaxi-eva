package cn.acecandy.fasaxi.eva.common.vo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.List;

/**
 * 请求头 VO
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Builder
@Data
public class HeadVO {
    private String ua;

    @SneakyThrows
    public static void main(String[] args) {
        List<String> existAnswer = CollUtil.newArrayList();
        List<Entity> buffer = CollUtil.newArrayList();
        Db db = Db.use();
        List<Entity> en = db.find(CollUtil.newArrayList("answer"), Entity.create("game_ktccy")
                .set("source", 5));
        en.forEach(e -> {
            existAnswer.add(e.getStr("answer"));
        });
        Console.log("添加{}个存在答案", existAnswer.size());
        String u = "https://free.wqwlkj.cn/wqwlapi/ccy.php";
        // String u = "https://api.lolimi.cn/API/ktcc/api.php";
        String res = "";
        JSONObject jn;
        String url;
        String answer;
        while (true) {
            try {
                res = HttpUtil.get(StrUtil.format(u, ""));
                jn = JSONUtil.parseObj(res);
                if (jn.getInt("code", 500) != 1) {
                    Console.log("请求失败,{}", res);
                    continue;
                }
                // jn = jn.getJSONObject("data");
                url = jn.getStr("img");
                answer = jn.getStr("answer");
                if (existAnswer.contains(answer)) {
                    System.out.print(".");
                    continue;
                } else {
                    Console.log("添加答案{}", answer);
                    existAnswer.add(answer);
                }
                buffer.add(Entity.create("game_ktccy")
                        .set("pic_url", url)
                        .set("answer", answer)
                        .set("source", 5));

                if (buffer.size() >= 10 || RandomUtil.randomInt(1, 10) < 4) {
                    db.insert(buffer);
                    CollUtil.clear(buffer);
                }
                ThreadUtil.safeSleep(20);
            } catch (cn.hutool.http.HttpException he) {
                System.err.print("X");
                ThreadUtil.safeSleep(5000);
            } catch (Exception e) {
                Console.error("插入失败", e);
            }
        }
    }
}