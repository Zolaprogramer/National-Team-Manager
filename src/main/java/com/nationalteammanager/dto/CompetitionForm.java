package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.enums.CompetitionType;
import com.nationalteammanager.validation.ValidDateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@ValidDateRange
public class CompetitionForm {

    @NotBlank(message = "{validation.competition.name.required}")
    @Size(max = 100, message = "{validation.competition.name.size}")
    private String name;

    @NotBlank(message = "{validation.competition.season.required}")
    @Size(max = 20, message = "{validation.competition.season.size}")
    private String season;

    @NotNull(message = "{validation.competition.type.required}")
    private CompetitionType type;

    @NotNull(message = "{validation.competition.start.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "{validation.competition.end.required}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private MultipartFile logo;

    private boolean removeLogo;

    private boolean shared;

    public MultipartFile getLogo() {
        return logo;
    }

    public void setLogo(MultipartFile logo) {
        this.logo = logo;
    }

    public boolean isRemoveLogo() {
        return removeLogo;
    }

    public void setRemoveLogo(boolean removeLogo) {
        this.removeLogo = removeLogo;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public static CompetitionForm from(Competition competition) {
        CompetitionForm form = new CompetitionForm();
        form.name = competition.getName();
        form.season = competition.getSeason();
        form.type = competition.getType();
        form.startDate = competition.getStartDate();
        form.endDate = competition.getEndDate();
        form.shared = competition.isShared();
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public CompetitionType getType() {
        return type;
    }

    public void setType(CompetitionType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
