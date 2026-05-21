package com.pet.walkthroughserver.modules.template.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.template.presentation.dto.CreateTemplateRequest;
import com.pet.walkthroughserver.modules.template.presentation.dto.UpdateTemplateRequest;
import com.pet.walkthroughserver.modules.template.repository.TemplateEntity;
import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;

public interface TemplateService {

    List<TemplateEntity> listVisible(UUID userId, TemplatePrType prType);

    TemplateEntity getById(UUID templateId, UUID userId);

    TemplateEntity create(UUID userId, CreateTemplateRequest request);

    TemplateEntity update(UUID templateId, UUID userId, UpdateTemplateRequest request);

    void delete(UUID templateId, UUID userId);

    TemplateEntity duplicate(UUID templateId, UUID userId, String overrideName);
}
