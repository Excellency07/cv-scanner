package com.backend.cv_scanner.service;

import com.backend.cv_scanner.dao.entity.Candidate;
import com.backend.cv_scanner.dao.repository.jpa.CandidateRepository;
import com.backend.cv_scanner.exception.CandidateNotFoundException;
import com.backend.cv_scanner.model.dto.PageResponseDto;
import com.backend.cv_scanner.model.dto.candidate.CandidateFilterDto;
import com.backend.cv_scanner.model.dto.candidate.CandidateResponseDto;
import com.backend.cv_scanner.model.enums.ExportFormat;
import com.backend.cv_scanner.model.mapper.CandidateMapper;
import com.backend.cv_scanner.specification.CandidateSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private static final int EXPORT_PAGE_SIZE = 500;

    private static final Pattern PLAIN_NUMBER = Pattern.compile("[+-]?[\\d ().-]+");

    private static final String[] EXPORT_HEADERS = {
            "Full Name", "Email", "Phone", "Years of Experience", "Skills", "Location",
            "Job Type", "Work Mode", "Source File", "Batch Id", "Created At"
    };

    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponseDto<CandidateResponseDto> search(CandidateFilterDto filter, Pageable pageable) {
        Specification<Candidate> specification = CandidateSpecification.getCandidateSpecification(filter);
        Page<Candidate> candidates = candidateRepository.findAll(specification, pageable);
        return PageResponseDto.toDto(candidates.map(candidateMapper::toDto));
    }

    @Transactional(readOnly = true)
    public CandidateResponseDto getById(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .map(candidateMapper::toDto)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found: " + candidateId));
    }

    @Transactional(readOnly = true)
    public void export(CandidateFilterDto filter, ExportFormat format, OutputStream outputStream) throws IOException {
        switch (format) {
            case CSV -> exportCsv(filter, outputStream);
            case XLSX -> exportXlsx(filter, outputStream);
        }
    }

    private void exportCsv(CandidateFilterDto filter, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        // BOM so that Excel opens UTF-8 names (e.g. "Əliyev", "Müller") correctly
        writer.write('﻿');

        CSVPrinter printer = CSVFormat.DEFAULT.builder().setHeader(EXPORT_HEADERS).get().print(writer);

        forEachCandidate(filter, candidate -> {
            try {
                printer.printRecord(toRow(candidate).stream().map(CandidateService::escapeFormula).toList());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write CSV export", e);
            }
        });

        printer.flush();
    }

    private void exportXlsx(CandidateFilterDto filter, OutputStream outputStream) throws IOException {
        // streaming workbook: keeps only 100 rows in memory, so large exports do not exhaust the heap
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Candidates");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);

            int[] rowIndex = {1};
            forEachCandidate(filter, candidate -> {
                Row row = sheet.createRow(rowIndex[0]++);
                List<String> values = toRow(candidate);
                for (int i = 0; i < values.size(); i++) {
                    if (i == 3 && candidate.yearsOfExperience() != null) {
                        row.createCell(i).setCellValue(candidate.yearsOfExperience());
                    } else {
                        row.createCell(i).setCellValue(values.get(i));
                    }
                }
            });

            workbook.write(outputStream);
        }
    }

    /**
     * Pages through all matching candidates, clearing the persistence context after each page
     * so memory stays flat even for exports of many thousands of rows.
     */
    private void forEachCandidate(CandidateFilterDto filter, Consumer<CandidateResponseDto> consumer) {
        Specification<Candidate> specification = CandidateSpecification.getCandidateSpecification(filter);
        Pageable pageable = PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by("createdAt", "id"));

        Page<Candidate> page;
        do {
            page = candidateRepository.findAll(specification, pageable);
            page.map(candidateMapper::toDto).forEach(consumer);
            entityManager.clear();
            pageable = page.nextPageable();
        } while (page.hasNext());
    }

    private static List<String> toRow(CandidateResponseDto candidate) {
        List<String> values = new ArrayList<>();
        values.add(candidate.fullName());
        values.add(candidate.email());
        values.add(candidate.phone());
        values.add(candidate.yearsOfExperience() != null ? candidate.yearsOfExperience().toString() : null);
        values.add(candidate.skills() != null ? String.join(", ", candidate.skills()) : null);
        values.add(candidate.location());
        values.add(candidate.jobType() != null ? candidate.jobType().name() : null);
        values.add(candidate.jobLocationType() != null ? candidate.jobLocationType().name() : null);
        values.add(candidate.sourceFilename());
        values.add(Objects.toString(candidate.batchId(), null));
        values.add(Objects.toString(candidate.createdAt(), null));
        return values.stream().map(value -> value == null ? "" : value).toList();
    }

    // CVs are untrusted input: a value like "=HYPERLINK(...)" must not run as a formula when the CSV is opened in Excel.
    // Plain numbers such as "+994 55 123 45 67" cannot contain a formula and are left readable.
    private static String escapeFormula(String value) {
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 && !PLAIN_NUMBER.matcher(value).matches()) {
            return "'" + value;
        }
        return value;
    }
}
