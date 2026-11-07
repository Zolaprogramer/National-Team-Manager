package com.nationalteammanager.dto;

import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.FlagChoice;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class NationalTeamForm {

    @NotBlank(message = "{validation.team.country.required}")
    @Size(max = 100, message = "{validation.team.country.size}")
    private String countryName;

    @NotBlank(message = "{validation.team.name.required}")
    @Size(max = 100, message = "{validation.team.name.size}")
    private String teamName;

    @NotNull(message = "{validation.team.confederation.required}")
    private Confederation confederation;

    @NotBlank(message = "{validation.team.coach.required}")
    @Size(max = 100, message = "{validation.team.coach.size}")
    private String coach;

    @NotNull(message = "{validation.team.year.required}")
    @Min(value = 1850, message = "{validation.team.year.min}")
    private Integer foundedYear;

    @Size(max = 1000, message = "{validation.team.description.size}")
    private String description;

    private MultipartFile flag;

    private FlagChoice flagChoice = FlagChoice.KEEP;

    private MultipartFile coachPhoto;

    private boolean removeCoachPhoto;

    public MultipartFile getFlag() {
        return flag;
    }

    public void setFlag(MultipartFile flag) {
        this.flag = flag;
    }

    public FlagChoice getFlagChoice() {
        return flagChoice;
    }

    public void setFlagChoice(FlagChoice flagChoice) {
        this.flagChoice = flagChoice;
    }

    public MultipartFile getCoachPhoto() {
        return coachPhoto;
    }

    public void setCoachPhoto(MultipartFile coachPhoto) {
        this.coachPhoto = coachPhoto;
    }

    public boolean isRemoveCoachPhoto() {
        return removeCoachPhoto;
    }

    public void setRemoveCoachPhoto(boolean removeCoachPhoto) {
        this.removeCoachPhoto = removeCoachPhoto;
    }

    public static NationalTeamForm from(NationalTeam team) {
        NationalTeamForm form = new NationalTeamForm();
        form.countryName = team.getCountryName();
        form.teamName = team.getTeamName();
        form.confederation = team.getConfederation();
        form.coach = team.getCoach();
        form.foundedYear = team.getFoundedYear();
        form.description = team.getDescription();
        return form;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Confederation getConfederation() {
        return confederation;
    }

    public void setConfederation(Confederation confederation) {
        this.confederation = confederation;
    }

    public String getCoach() {
        return coach;
    }

    public void setCoach(String coach) {
        this.coach = coach;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
