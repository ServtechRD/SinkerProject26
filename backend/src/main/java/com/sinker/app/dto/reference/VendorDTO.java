package com.sinker.app.dto.reference;

import com.sinker.app.entity.Vendor;

public class VendorDTO {

    private String code;
    private String name;

    public VendorDTO() {}

    public static VendorDTO fromEntity(Vendor entity) {
        VendorDTO dto = new VendorDTO();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        return dto;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
