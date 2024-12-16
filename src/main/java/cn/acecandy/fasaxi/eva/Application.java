package cn.acecandy.fasaxi.eva;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.acecandy.fasaxi.eva.runtime.Task;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

@Slf4j
@RestController
@SpringBootApplication
public class Application {

    @SneakyThrows
    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(Application.class, args);
        init();

        Environment env = application.getEnvironment();
        String host = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port", "8801");
        String pathContext = env.getProperty("server.servlet.context-path", "");
        String uri = StrUtil.format("http://{}:{}{}", host, port, pathContext);
        log.info("""
                 \r----------------------------------------------------------
                 {}[{}] 已启动!
                 请求路径: {}
                 健康检查: {}/health/time
                 接口文档: {}/doc.html
                 ----------------------------------------------------------
                 """,
                env.getProperty("spring.application.name"), env.getProperty("spring.profiles.active"),
                uri, uri, uri);
    }

    /**
     * 提前初始化一些数据
     */
    private static void init() {
        ThreadUtil.execAsync(() -> SpringUtil.getBean(Task.class).run());
    }
}