package com.hanserwei.hannote.note.biz.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.api.KeyValueFeignApi;
import com.hanserwei.hannote.kv.dto.req.AddNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.DeleteNoteContentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    /**
     * 保存笔记内容
     *
     * @param uuid 笔记UUID
     * @param content 笔记内容
     * @return 是否成功
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentReqDTO addNoteContentReqDTO = new AddNoteContentReqDTO();
        addNoteContentReqDTO.setUuid(uuid);
        addNoteContentReqDTO.setContent(content);

        Response<?> response = keyValueFeignApi.addNoteContent(addNoteContentReqDTO);

        return Objects.nonNull(response) && response.isSuccess();
    }

    /**
     * 删除笔记内容
     *
     * @param uuid 笔记UUID
     * @return 是否成功
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = new DeleteNoteContentReqDTO();
        deleteNoteContentReqDTO.setUuid(uuid);

        Response<?> response = keyValueFeignApi.deleteNoteContent(deleteNoteContentReqDTO);

        return Objects.nonNull(response) && response.isSuccess();
    }

}