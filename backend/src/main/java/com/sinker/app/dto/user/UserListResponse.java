package com.sinker.app.dto.user;

import java.util.List;

public class UserListResponse {

    private List<UserDTO> users;
    private long totalElements;
    private int totalPages;
    private int currentPage;

    public UserListResponse() {}

    public UserListResponse(List<UserDTO> users, long totalElements, int totalPages, int currentPage) {
        this.users = users;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public List<UserDTO> getUsers() { return users; }
    public void setUsers(List<UserDTO> users) { this.users = users; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
}
