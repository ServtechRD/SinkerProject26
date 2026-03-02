package com.sinker.app.dto.reference;

import com.sinker.app.entity.Category;

public class CategoryDTO {

    private Integer id;
    private String name;

    public CategoryDTO() {}

    public static CategoryDTO fromEntity(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
