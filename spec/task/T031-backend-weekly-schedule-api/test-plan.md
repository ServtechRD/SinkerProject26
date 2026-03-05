# T031: Test Plan

## Unit Tests

### WeeklyScheduleServiceTest.java
- **testValidateMondaySuccess**: Verify Monday dates pass validation
- **testValidateMondayFailure**: Verify non-Monday dates fail
- **testDeleteExistingData**: Verify delete for week+factory combination
- **testSaveScheduleData**: Verify save logic
- **testUpdateSchedule**: Verify update demand_date and quantity
- **testRecordNotFound**: Verify exception for invalid ID

### WeeklyScheduleExcelParserTest.java
- **testParseValidExcel**: Parse Excel with all required columns
- **testParseMissingColumns**: Verify error when columns missing
- **testParseEmptyRows**: Verify empty rows skipped
- **testParseChineseHeaders**: Verify Chinese column headers recognized
- **testParseQuantityDecimal**: Verify decimal parsing preserves precision
- **testParseDateFormat**: Verify date parsing handles Excel date formats
- **testParseXlsFormat**: Verify .xls (old format) supported
- **testParseXlsxFormat**: Verify .xlsx (new format) supported
- **testParseInvalidFile**: Verify error for corrupted/non-Excel files

### WeeklyScheduleControllerTest.java (WebMvcTest)
- **testUploadEndpoint**: Verify multipart upload handling
- **testGetSchedules**: Verify GET with query parameters
- **testPutSchedule**: Verify PUT endpoint
- **testUploadNotMonday**: Verify 400 for non-Monday
- **testUploadMissingFile**: Verify 400 when file missing
- **testGetMissingParams**: Verify 400 when parameters missing
- **testUnauthorized**: Verify 401 without JWT
- **testForbiddenUpload**: Verify 403 without upload permission
- **testForbiddenEdit**: Verify 403 without edit permission

## Integration Tests

### WeeklyScheduleIT.java
- **testUploadExcelFlow**: Full flow from file upload to database
- **testUploadReplacesExisting**: Verify second upload deletes first
- **testUploadTransactionRollback**: Simulate error mid-upload, verify rollback
- **testQueryAfterUpload**: Upload then query, verify data matches
- **testEditAfterUpload**: Upload then edit, verify changes persist
- **testMultipleFactoriesSameWeek**: Upload to different factories, verify isolation
- **testMultipleWeeksSameFactory**: Upload different weeks, verify all retained
- **test1000RowUpload**: Performance test with large Excel file

### Setup
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class WeeklyScheduleIT {

    @Container
    static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:10.11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeeklyScheduleRepository repository;

    private String jwtToken;

    @BeforeEach
    void setup() {
        jwtToken = createJwtToken("testuser",
            "weekly_schedule.upload",
            "weekly_schedule.view",
            "weekly_schedule.edit");

        repository.deleteAll();
    }

    @Test
    void testUploadExcel() throws Exception {
        // Create test Excel file
        MockMultipartFile file = createTestExcelFile();

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                .file(file)
                .param("week_start", "2026-02-02")
                .param("factory", "FACTORY-A")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsInserted").value(3));

        List<WeeklySchedule> schedules = repository.findByWeekStartAndFactory(
            LocalDate.of(2026, 2, 2), "FACTORY-A");
        assertEquals(3, schedules.size());
    }

    private MockMultipartFile createTestExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Schedule");

        // Header row (Chinese)
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("需求日期");
        headerRow.createCell(1).setCellValue("品號");
        headerRow.createCell(2).setCellValue("品名");
        headerRow.createCell(3).setCellValue("庫位");
        headerRow.createCell(4).setCellValue("箱數小計");

        // Data rows
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("2026-02-03");
        row1.createCell(1).setCellValue("PROD001");
        row1.createCell(2).setCellValue("Product 1");
        row1.createCell(3).setCellValue("WH-A");
        row1.createCell(4).setCellValue(100.50);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        return new MockMultipartFile("file", "schedule.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            bos.toByteArray());
    }
}
```

## E2E Tests

### weekly-schedule.spec.js (Playwright)
- **testUploadFlow**: Login, navigate to page, upload file, verify success
- **testEditAfterUpload**: Upload, then edit row, verify changes
- **testMultipleUploads**: Upload twice, verify second replaces first
- **testPermissionDenied**: Login without permission, verify upload disabled

## Test Data Setup Notes

### Excel Test File Structure
```
需求日期      | 品號     | 品名       | 庫位  | 箱數小計
2026-02-03   | PROD001  | Product 1  | WH-A  | 100.50
2026-02-04   | PROD002  | Product 2  | WH-B  | 150.75
2026-02-05   | PROD003  | Product 3  | WH-C  | 200.00
```

### Monday Test Dates
```java
// Valid Mondays
LocalDate.of(2026, 2, 2)  // Monday
LocalDate.of(2026, 2, 9)  // Monday
LocalDate.of(2026, 2, 16) // Monday

// Invalid (non-Monday)
LocalDate.of(2026, 2, 1)  // Sunday
LocalDate.of(2026, 2, 3)  // Tuesday
LocalDate.of(2026, 2, 4)  // Wednesday
```

### User Permissions
```sql
-- User with all permissions
INSERT INTO user_roles VALUES
  ((SELECT id FROM users WHERE username = 'scheduler'),
   (SELECT id FROM roles WHERE name = 'SCHEDULE_MANAGER'));

-- User with view only
INSERT INTO user_roles VALUES
  ((SELECT id FROM users WHERE username = 'viewer'),
   (SELECT id FROM roles WHERE name = 'SCHEDULE_VIEWER'));
```

## Mocking Strategy

### Unit Tests
- Mock WeeklyScheduleRepository
- Mock Excel file with MockMultipartFile
- Mock Apache POI Workbook for parser tests
- Use @MockBean for Spring components

### Integration Tests
- Real database via Testcontainers
- Real Excel files created programmatically
- Real Spring Security with test JWT
- No external service mocks

### E2E Tests
- Real backend and database
- Real Excel file upload
- Real authentication

## Performance Benchmarks
- Parse 100-row Excel: < 1 second
- Parse 1000-row Excel: < 5 seconds
- Upload + delete + insert 100 rows: < 2 seconds
- Upload + delete + insert 1000 rows: < 10 seconds
- Query 1000 rows: < 500ms
- Single edit: < 300ms
