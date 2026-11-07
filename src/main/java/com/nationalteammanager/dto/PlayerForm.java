package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.PlayerPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class PlayerForm {

    @NotBlank(message = "{validation.player.firstName.required}")
    @Size(max = 50, message = "{validation.player.firstName.size}")
    private String firstName;

    @NotBlank(message = "{validation.player.lastName.required}")
    @Size(max = 50, message = "{validation.player.lastName.size}")
    private String lastName;

    @NotNull(message = "{validation.player.birthDate.required}")
    @Past(message = "{validation.player.birthDate.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotBlank(message = "{validation.player.nationality.required}")
    @Size(max = 100, message = "{validation.player.nationality.size}")
    private String nationality;

    @NotNull(message = "{validation.player.position.required}")
    private PlayerPosition position;

    @NotNull(message = "{validation.player.jersey.required}")
    @Min(value = 1, message = "{validation.player.jersey.range}")
    @Max(value = 99, message = "{validation.player.jersey.range}")
    private Integer jerseyNumber;

    @NotNull(message = "{validation.player.height.required}")
    @Min(value = 140, message = "{validation.player.height.range}")
    @Max(value = 230, message = "{validation.player.height.range}")
    private Integer heightCm;

    @Size(max = 100, message = "{validation.player.club.size}")
    private String club;

    private MultipartFile photo;

    private boolean removePhoto;

    public static PlayerForm from(Player player) {
        PlayerForm form = new PlayerForm();
        form.firstName = player.getFirstName();
        form.lastName = player.getLastName();
        form.birthDate = player.getBirthDate();
        form.nationality = player.getNationality();
        form.position = player.getPosition();
        form.jerseyNumber = player.getJerseyNumber();
        form.heightCm = player.getHeightCm();
        form.club = player.getClub();
        return form;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public void setPosition(PlayerPosition position) {
        this.position = position;
    }

    public Integer getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(Integer jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public String getClub() {
        return club;
    }

    public void setClub(String club) {
        this.club = club;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }

    public boolean isRemovePhoto() {
        return removePhoto;
    }

    public void setRemovePhoto(boolean removePhoto) {
        this.removePhoto = removePhoto;
    }
}
