package com.hanserwei.hannote.kv.biz.service.impl;

import com.hanserwei.framework.common.exception.ApiException;
import com.hanserwei.framework.common.response.Response;
import com.hanserwei.hannote.kv.biz.domain.dataobject.NoteContentDO;
import com.hanserwei.hannote.kv.biz.domain.repository.NoteContentRepository;
import com.hanserwei.hannote.kv.biz.enums.ResponseCodeEnum;
import com.hanserwei.hannote.kv.biz.service.NoteContentService;
import com.hanserwei.hannote.kv.dto.req.AddNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.DeleteNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.req.FindNoteContentReqDTO;
import com.hanserwei.hannote.kv.dto.resp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;

    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        // 笔记ID
        String noteId = addNoteContentReqDTO.getUuid();
        // 笔记内容
        String content = addNoteContentReqDTO.getContent();

        NoteContentDO noteContent = NoteContentDO.builder()
                .id(UUID.fromString(noteId))
                .content(content)
                .build();

        // 插入数据
        noteContentRepository.save(noteContent);
        return Response.success();
    }

    @Override
    public Response<FindNoteContentRspDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        // 笔记ID
        String noteId = findNoteContentReqDTO.getUuid();
        Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(noteId));
        if (optional.isEmpty()){
            throw new ApiException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }
        NoteContentDO noteContentDO = optional.get();
        // 构建回参
        FindNoteContentRspDTO findNoteContentRspDTO = FindNoteContentRspDTO.builder()
                .noteId(noteContentDO.getId())
                .content(noteContentDO.getContent())
                .build();
        return Response.success(findNoteContentRspDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        String noteId = deleteNoteContentReqDTO.getUuid();
        noteContentRepository.deleteById(UUID.fromString(noteId));
        return Response.success();
    }
}
