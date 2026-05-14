package com.smartmailer.recipient;

import com.smartmailer.campaign.Campaign;
import com.smartmailer.campaign.CampaignService;
import com.smartmailer.campaign.CampaignStatus;
import com.smartmailer.common.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static com.smartmailer.recipient.RecipientDtos.*;

@Service
public class RecipientService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> BUILT_IN = Set.of("name", "email", "company", "role");

    private final CampaignService campaigns;
    private final RecipientRepository recipients;
    private final RecipientMapper mapper;
    private final TemplateRenderingService renderer;

    public RecipientService(CampaignService campaigns, RecipientRepository recipients, RecipientMapper mapper, TemplateRenderingService renderer) {
        this.campaigns = campaigns;
        this.recipients = recipients;
        this.mapper = mapper;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipientResponse> page(Long campaignId, int page, int size, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        var result = recipients.findByCampaignOrderByIdAsc(campaign, PageRequest.of(Math.max(page - 1, 0), Math.min(size, 100)));
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public UploadSummary upload(Long campaignId, MultipartFile file, Authentication auth) {
        Campaign campaign = campaigns.requireCampaign(campaignId, auth);
        List<Map<String, String>> rows = parse(file);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload contains no rows");
        }
        Set<String> headers = rows.getFirst().keySet();
        if (!headers.contains("name") || !headers.contains("email")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CSV/Excel must contain name and email columns");
        }
        renderer.validate(campaign.getSubjectTemplate(), campaign.getBodyTemplate(), headers);

        List<UploadError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int rowNumber = 1;
        int saved = 0;
        for (Map<String, String> row : rows) {
            rowNumber++;
            String email = value(row, "email").toLowerCase();
            if (value(row, "name").isBlank() || email.isBlank()) {
                errors.add(new UploadError(rowNumber, email, "Missing required name or email"));
                continue;
            }
            if (!EMAIL.matcher(email).matches()) {
                errors.add(new UploadError(rowNumber, email, "Invalid email address"));
                continue;
            }
            if (!seen.add(email) || recipients.existsByCampaignAndEmail(campaign, email)) {
                errors.add(new UploadError(rowNumber, email, "Duplicate email in campaign/upload"));
                continue;
            }

            Map<String, String> custom = new LinkedHashMap<>();
            row.forEach((key, val) -> {
                if (!BUILT_IN.contains(key)) {
                    custom.put(key, val);
                }
            });
            recipients.save(Recipient.builder()
                    .campaign(campaign)
                    .name(value(row, "name"))
                    .email(email)
                    .company(value(row, "company"))
                    .role(value(row, "role"))
                    .customDataJson(custom)
                    .renderedSubject(renderer.render(campaign.getSubjectTemplate(), row))
                    .renderedBody(renderer.render(campaign.getBodyTemplate(), row))
                    .status(RecipientStatus.PENDING)
                    .build());
            saved++;
        }
        campaign.setStatus(CampaignStatus.PENDING);
        return new UploadSummary(saved, errors.size(), errors);
    }

    @Transactional
    public RecipientResponse update(Long id, RecipientUpdateRequest request, Authentication auth) {
        Recipient recipient = requireOwnedRecipient(id, auth);
        recipient.setName(request.name());
        recipient.setEmail(request.email().trim().toLowerCase());
        recipient.setCompany(request.company());
        recipient.setRole(request.role());
        recipient.setCustomDataJson(request.customDataJson());
        recipient.setRenderedSubject(request.renderedSubject());
        recipient.setRenderedBody(request.renderedBody());
        return mapper.toResponse(recipient);
    }

    @Transactional
    public RecipientResponse approve(Long id, Authentication auth) {
        Recipient recipient = requireOwnedRecipient(id, auth);
        recipient.setStatus(RecipientStatus.APPROVED);
        return mapper.toResponse(recipient);
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        requireOwnedRecipient(id, auth).setStatus(RecipientStatus.DELETED);
    }

    @Transactional
    public RecipientResponse restore(Long id, Authentication auth) {
        Recipient recipient = requireOwnedRecipient(id, auth);
        recipient.setStatus(RecipientStatus.PENDING);
        return mapper.toResponse(recipient);
    }

    public Recipient requireOwnedRecipient(Long id, Authentication auth) {
        Recipient recipient = recipients.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient not found"));
        campaigns.requireCampaign(recipient.getCampaign().getId(), auth);
        return recipient;
    }

    private List<Map<String, String>> parse(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(file);
            }
            return parseCsv(file);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to parse upload: " + ex.getMessage());
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws Exception {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), format)) {
            List<String> headers = parser.getHeaderNames().stream().map(String::trim).toList();
            List<Map<String, String>> rows = new ArrayList<>();
            parser.forEach(record -> {
                Map<String, String> row = new LinkedHashMap<>();
                headers.forEach(header -> row.put(header, record.get(header).trim()));
                rows.add(row);
            });
            return rows;
        }
    }

    private List<Map<String, String>> parseExcel(MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> iterator = sheet.rowIterator();
            if (!iterator.hasNext()) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            iterator.next().forEach(cell -> headers.add(cell.getStringCellValue().trim()));
            List<Map<String, String>> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            while (iterator.hasNext()) {
                Row excelRow = iterator.next();
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), formatter.formatCellValue(excelRow.getCell(i)).trim());
                }
                rows.add(row);
            }
            return rows;
        }
    }

    private String value(Map<String, String> row, String key) {
        return row.getOrDefault(key, "").trim();
    }
}
