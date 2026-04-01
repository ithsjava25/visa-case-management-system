package org.example.visacasemanagementsystem.visa.service;

import org.example.visacasemanagementsystem.visa.VisaStatus;
import org.example.visacasemanagementsystem.visa.dto.VisaDTO;
import org.example.visacasemanagementsystem.visa.entity.Visa;
import org.example.visacasemanagementsystem.visa.mapper.VisaMapper;
import org.example.visacasemanagementsystem.visa.repository.VisaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisaService {

    private final VisaRepository visaRepository;
    private final VisaMapper visaMapper;

    public VisaService(VisaRepository visaRepository, VisaMapper visaMapper) {
        this.visaRepository = visaRepository;
        this.visaMapper = visaMapper;
    }

    public List<Visa> findAll() {
        return visaRepository.findAll();
    }

    public Optional<Visa> findById(Long id) {
        return visaRepository.findById(id);
    }

    public List<VisaDTO> findByVisaType(String visaType) {
       return visaRepository.findByVisaTypeContainingIgnoreCase(visaType, Sort.by("visaType").descending())
               .stream()
               .map(visaMapper::toDTO)
               .toList();
    }

    public List<VisaDTO> findByVisaStatus(String visaStatus) {
        VisaStatus status = VisaStatus.valueOf(visaStatus.toUpperCase());

        List<Visa> visaEntites = visaRepository.findByVisaStatus(
                status,
                Sort.by("visaType").descending()
        );

        return visaEntites.stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findByDate(LocalDate date){
        LocalDateTime startofDay = date.atStartOfDay();
        LocalDateTime endofDay = date.atTime(LocalTime.MAX);

        return visaRepository.findByCreatedAtBetween(
                startofDay,
                endofDay,
                Sort.by("createdAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();
    }

    public List<VisaDTO> findByUpdate(LocalDateTime since){
        return visaRepository.findByUpdatedAtAfter(since, Sort.by("updatedAt").descending())
                .stream()
                .map(visaMapper::toDTO)
                .toList();

    }


}
