package cn.acecandy.fasaxi.eva.bean.resp;

import lombok.Data;

import java.util.List;

/**
 * 视频信息 出参
 *
 * @author tangningzhu
 * @since 2024/11/22
 */
@Data
public class EmbyItemsResp {
    /**
     * emby项目
     */
    private List<EmbyItems> items;
    /**
     * 总记录数
     */
    private Integer totalRecordCount;

    @Data
    public static class EmbyItems {
        /**
         * 文件名称
         */
        private String name;
        /**
         * 文件id
         */
        private String id;
        /**
         * 媒体来源信息
         */
        private List<MediaSources> mediaSources;
        /**
         * 本地路径
         */
        private String path;

        @Data
        public static class MediaSources {
            /**
             * strm具体路径
             */
            private String path;
        }
    }
}