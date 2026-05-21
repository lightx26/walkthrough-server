package com.pet.walkthroughserver.modules.template.business.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.template.exceptions.BuiltinTemplateModificationException;
import com.pet.walkthroughserver.modules.template.exceptions.TemplateAccessDeniedException;
import com.pet.walkthroughserver.modules.template.exceptions.TemplateNotFoundException;
import com.pet.walkthroughserver.modules.template.presentation.dto.CreateTemplateRequest;
import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateChapterRequest;
import com.pet.walkthroughserver.modules.template.presentation.dto.UpdateTemplateRequest;
import com.pet.walkthroughserver.modules.template.repository.TemplateChapterEntity;
import com.pet.walkthroughserver.modules.template.repository.TemplateEntity;
import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;
import com.pet.walkthroughserver.modules.template.repository.TemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateEntity> listVisible(UUID userId, TemplatePrType prType) {
        if (prType != null) {
            return templateRepository.findVisibleToUserByPrType(userId, prType);
        }
        return templateRepository.findVisibleToUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateEntity getById(UUID templateId, UUID userId) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        assertReadable(template, userId);
        return template;
    }

    @Override
    @Transactional
    public TemplateEntity create(UUID userId, CreateTemplateRequest request) {
        TemplateEntity template = TemplateEntity.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .prType(request.getPrType())
                .isBuiltin(false)
                .chapters(new ArrayList<>())
                .build();

        if (request.getChapters() != null) {
            for (TemplateChapterRequest chapterRequest : request.getChapters()) {
                TemplateChapterEntity chapter = TemplateChapterEntity.builder()
                        .template(template)
                        .title(chapterRequest.getTitle())
                        .description(chapterRequest.getDescription())
                        .sortOrder(chapterRequest.getSortOrder())
                        .build();
                template.getChapters().add(chapter);
            }
        }

        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public TemplateEntity update(UUID templateId, UUID userId, UpdateTemplateRequest request) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        assertOwned(template, userId);

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getPrType() != null) {
            template.setPrType(request.getPrType());
        }
        if (request.getChapters() != null) {
            template.getChapters().clear();
            for (TemplateChapterRequest chapterRequest : request.getChapters()) {
                TemplateChapterEntity chapter = TemplateChapterEntity.builder()
                        .template(template)
                        .title(chapterRequest.getTitle())
                        .description(chapterRequest.getDescription())
                        .sortOrder(chapterRequest.getSortOrder())
                        .build();
                template.getChapters().add(chapter);
            }
        }

        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void delete(UUID templateId, UUID userId) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        assertOwned(template, userId);
        templateRepository.delete(template);
    }

    @Override
    @Transactional
    public TemplateEntity duplicate(UUID templateId, UUID userId, String overrideName) {
        TemplateEntity source = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template not found: " + templateId));
        assertReadable(source, userId);

        String newName = (overrideName != null && !overrideName.isBlank())
                ? overrideName
                : source.getName() + " (copy)";

        TemplateEntity copy = TemplateEntity.builder()
                .userId(userId)
                .name(newName)
                .description(source.getDescription())
                .prType(source.getPrType())
                .isBuiltin(false)
                .chapters(new ArrayList<>())
                .build();

        for (TemplateChapterEntity sourceChapter : source.getChapters()) {
            TemplateChapterEntity chapter = TemplateChapterEntity.builder()
                    .template(copy)
                    .title(sourceChapter.getTitle())
                    .description(sourceChapter.getDescription())
                    .sortOrder(sourceChapter.getSortOrder())
                    .build();
            copy.getChapters().add(chapter);
        }

        return templateRepository.save(copy);
    }

    private void assertReadable(TemplateEntity template, UUID userId) {
        if (Boolean.TRUE.equals(template.getIsBuiltin())) {
            return;
        }
        if (template.getUserId() == null || !template.getUserId().equals(userId)) {
            throw new TemplateAccessDeniedException("Template not accessible: " + template.getId());
        }
    }

    private void assertOwned(TemplateEntity template, UUID userId) {
        if (Boolean.TRUE.equals(template.getIsBuiltin())) {
            throw new BuiltinTemplateModificationException(
                    "Built-in templates cannot be modified or deleted");
        }
        if (template.getUserId() == null || !template.getUserId().equals(userId)) {
            throw new TemplateAccessDeniedException("Template not owned by user: " + template.getId());
        }
    }
}
