package com.hanserwei.hannote.search.api;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.search.constant.ApiConstants;
import com.hanserwei.hannote.search.dto.RebuildNoteDocumentReqDTO;
import com.hanserwei.hannote.search.dto.RebuildUserDocumentReqDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface SearchFeignApi {

    String PREFIX = "/search";

    /**
     * 重建笔记文档
     *
     * @param rebuildNoteDocumentReqDTO 重建笔记文档请求参数对象，包含重建所需的相关信息
     * @return 返回重建操作的结果响应对象
     */
    @PostMapping(value = PREFIX + "/note/document/rebuild")
    Response<?> rebuildNoteDocument(@RequestBody RebuildNoteDocumentReqDTO rebuildNoteDocumentReqDTO);


    /**
     * 重建用户文档
     *
     * @param rebuildUserDocumentReqDTO 重建用户文档请求参数对象，包含重建所需的相关信息
     * @return 返回重建操作的结果响应对象
     */
    @PostMapping(value = PREFIX + "/user/document/rebuild")
    Response<?> rebuildUserDocument(@RequestBody RebuildUserDocumentReqDTO rebuildUserDocumentReqDTO);

}