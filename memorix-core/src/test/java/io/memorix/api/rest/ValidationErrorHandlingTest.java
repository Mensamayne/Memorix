package io.memorix.api.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite for BUG #5: ERROR MESSAGES ARE 500 INSTEAD OF 400
 * 
 * <p>Validates that the API returns proper 400 Bad Request errors with descriptive
 * messages for invalid input instead of 500 Internal Server Error.
 * 
 * <p>Test cases from API_TESTING_REPORT.md:
 * <ul>
 *   <li>Empty content → 400 (not 500)</li>
 *   <li>Null/empty userId → 400 (not 500)</li>
 *   <li>Invalid pluginType → 400 (not 500)</li>
 *   <li>Empty query string → 400 (not 500)</li>
 *   <li>maxCount=0 → 400 (not 500)</li>
 * </ul>
 */
@SpringBootTest(classes = io.memorix.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ValidationErrorHandlingTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    // ============================================
    // POST /memories VALIDATION TESTS
    // ============================================
    
    @Test
    public void testEmptyContent_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("content")))
                .andExpect(jsonPath("$.message").value(containsString("empty")))
                .andExpect(jsonPath("$.field").value("content"))
                .andExpect(jsonPath("$.expectedFormat").exists());
    }
    
    @Test
    public void testNullContent_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": null,
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("content")))
                .andExpect(jsonPath("$.field").value("content"));
    }
    
    @Test
    public void testEmptyUserId_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "",
                "content": "Some content",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("userId")))
                .andExpect(jsonPath("$.field").value("userId"));
    }
    
    @Test
    public void testNullUserId_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": null,
                "content": "Some content",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("userId")))
                .andExpect(jsonPath("$.field").value("userId"));
    }
    
    @Test
    public void testInvalidPluginType_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "Some content",
                "pluginType": "INVALID_PLUGIN_TYPE_XYZ",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("pluginType")))
                .andExpect(jsonPath("$.message").value(containsString("INVALID_PLUGIN_TYPE_XYZ")))
                .andExpect(jsonPath("$.field").value("pluginType"))
                .andExpect(jsonPath("$.expectedFormat").exists())
                .andExpect(jsonPath("$.expectedFormat").value(containsString("USER_PREFERENCE")));
    }
    
    @Test
    public void testEmptyPluginType_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "Some content",
                "pluginType": "",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("pluginType")))
                .andExpect(jsonPath("$.field").value("pluginType"));
    }
    
    @Test
    public void testNullPluginType_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "Some content",
                "pluginType": null,
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("pluginType")))
                .andExpect(jsonPath("$.field").value("pluginType"));
    }
    
    @Test
    public void testImportanceTooHigh_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "Some content",
                "pluginType": "USER_PREFERENCE",
                "importance": 1.5
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("importance")))
                .andExpect(jsonPath("$.message").value(containsString("0.0 and 1.0")))
                .andExpect(jsonPath("$.field").value("importance"))
                .andExpect(jsonPath("$.rejectedValue").value(1.5))
                .andExpect(jsonPath("$.expectedFormat").value(containsString("[0.0, 1.0]")));
    }
    
    @Test
    public void testImportanceNegative_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "content": "Some content",
                "pluginType": "USER_PREFERENCE",
                "importance": -0.5
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("importance")))
                .andExpect(jsonPath("$.message").value(containsString("0.0 and 1.0")))
                .andExpect(jsonPath("$.field").value("importance"))
                .andExpect(jsonPath("$.rejectedValue").value(-0.5))
                .andExpect(jsonPath("$.expectedFormat").value(containsString("[0.0, 1.0]")));
    }
    
    @Test
    public void testImportanceBoundaryMin_ShouldReturn200() throws Exception {
        String requestBody = """
            {
                "userId": "test-user-boundary-min",
                "content": "Content with minimum importance",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.0
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importance").value(0.0));
    }
    
    @Test
    public void testImportanceBoundaryMax_ShouldReturn200() throws Exception {
        String requestBody = """
            {
                "userId": "test-user-boundary-max",
                "content": "Content with maximum importance",
                "pluginType": "USER_PREFERENCE",
                "importance": 1.0
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importance").value(1.0));
    }
    
    // ============================================
    // POST /memories/search VALIDATION TESTS
    // ============================================
    
    @Test
    public void testSearchEmptyQuery_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "query": "",
                "pluginType": "USER_PREFERENCE",
                "maxCount": 10
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("query")))
                .andExpect(jsonPath("$.message").value(containsString("empty")))
                .andExpect(jsonPath("$.field").value("query"));
    }
    
    @Test
    public void testSearchNullQuery_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "query": null,
                "pluginType": "USER_PREFERENCE",
                "maxCount": 10
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("query")))
                .andExpect(jsonPath("$.field").value("query"));
    }
    
    @Test
    public void testSearchEmptyUserId_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "",
                "query": "test query",
                "pluginType": "USER_PREFERENCE",
                "maxCount": 10
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("userId")))
                .andExpect(jsonPath("$.field").value("userId"));
    }
    
    @Test
    public void testSearchMaxCountZero_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "query": "test query",
                "pluginType": "USER_PREFERENCE",
                "maxCount": 0
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("maxCount")))
                .andExpect(jsonPath("$.message").value(containsString("greater than 0")))
                .andExpect(jsonPath("$.field").value("maxCount"));
    }
    
    @Test
    public void testSearchMaxCountNegative_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "query": "test query",
                "pluginType": "USER_PREFERENCE",
                "maxCount": -5
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("maxCount")))
                .andExpect(jsonPath("$.field").value("maxCount"));
    }
    
    @Test
    public void testSearchInvalidPluginType_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "userId": "test-user",
                "query": "test query",
                "pluginType": "INVALID_TYPE",
                "maxCount": 10
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("pluginType")))
                .andExpect(jsonPath("$.field").value("pluginType"));
    }
    
    // ============================================
    // POSITIVE TESTS (should still work)
    // ============================================
    
    @Test
    public void testValidRequest_ShouldReturn200() throws Exception {
        String requestBody = """
            {
                "userId": "test-validation-user",
                "content": "Valid content for validation test",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Valid content for validation test"))
                .andExpect(jsonPath("$.userId").value("test-validation-user"));
    }
    
    @Test
    public void testValidSearchRequest_ShouldReturn200() throws Exception {
        // First save a memory
        String saveBody = """
            {
                "userId": "search-validation-user",
                "content": "Searchable content",
                "pluginType": "USER_PREFERENCE",
                "importance": 0.8
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody))
                .andExpect(status().isOk());
        
        // Then search
        String searchBody = """
            {
                "userId": "search-validation-user",
                "query": "searchable",
                "pluginType": "USER_PREFERENCE",
                "maxCount": 10
            }
            """;
        
        mockMvc.perform(post("/api/memorix/memories/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memories").isArray());
    }
}

