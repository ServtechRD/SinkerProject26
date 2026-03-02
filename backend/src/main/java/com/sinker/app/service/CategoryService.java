package com.sinker.app.service;

import com.sinker.app.dto.reference.CategoryDTO;
import com.sinker.app.entity.Category;
import com.sinker.app.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDTO> findAll() {
        List<Category> list = categoryRepository.findAllByOrderByNameAsc();
        return list.stream().map(CategoryDTO::fromEntity).collect(Collectors.toList());
    }
}
