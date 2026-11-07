package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.enums.StadiumSurface;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class StadiumForm {

    @NotNull(message = "{validation.stadium.team.required}")
    private Long teamId;

    @NotBlank(message = "{validation.stadium.name.required}")
    @Size(max = 100, message = "{validation.stadium.name.size}")
    private String name;

    @NotBlank(message = "{validation.stadium.city.required}")
    @Size(max = 100, message = "{validation.stadium.city.size}")
    private String city;

    @NotBlank(message = "{validation.stadium.country.required}")
    @Size(max = 100, message = "{validation.stadium.country.size}")
    private String country;

    @NotNull(message = "{validation.stadium.capacity.required}")
    @Min(value = 1, message = "{validation.stadium.capacity.range}")
    @Max(value = 300000, message = "{validation.stadium.capacity.range}")
    private Integer capacity;

    @NotNull(message = "{validation.stadium.builtYear.required}")
    @Min(value = 1800, message = "{validation.stadium.builtYear.min}")
    private Integer builtYear;

    @NotNull(message = "{validation.stadium.surface.required}")
    private StadiumSurface surface;

    @NotBlank(message = "{validation.stadium.address.required}")
    @Size(max = 200, message = "{validation.stadium.address.size}")
    private String address;

    @Size(max = 1000, message = "{validation.stadium.description.size}")
    private String description;

    private boolean mainStadium;

    private MultipartFile photo;

    private boolean removePhoto;

    public static StadiumForm from(Stadium stadium) {
        StadiumForm form = new StadiumForm();
        form.teamId = stadium.getTeam().getId();
        form.name = stadium.getName();
        form.city = stadium.getCity();
        form.country = stadium.getCountry();
        form.capacity = stadium.getCapacity();
        form.builtYear = stadium.getBuiltYear();
        form.surface = stadium.getSurface();
        form.address = stadium.getAddress();
        form.description = stadium.getDescription();
        form.mainStadium = stadium.isMainStadium();
        return form;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBuiltYear() {
        return builtYear;
    }

    public void setBuiltYear(Integer builtYear) {
        this.builtYear = builtYear;
    }

    public StadiumSurface getSurface() {
        return surface;
    }

    public void setSurface(StadiumSurface surface) {
        this.surface = surface;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isMainStadium() {
        return mainStadium;
    }

    public void setMainStadium(boolean mainStadium) {
        this.mainStadium = mainStadium;
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
