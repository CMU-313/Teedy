package com.sismics.docs.rest;

import com.sismics.util.filter.TokenBasedSecurityFilter;
import org.junit.Assert;
import org.junit.Test;

import javax.json.JsonObject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Exhaustive test of the vocabulary resource.
 * 
 * @author bgamard
 */
public class TestVocabularyResource extends BaseJerseyTest {
    /**
     * Test the vocabulary resource.
     */
    @Test
    public void testVocabularyResource() {
        // Login vocabulary1
        clientUtil.createUser("vocabulary1");
        String vocabulary1Token = clientUtil.login("vocabulary1");
        
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);
        
        // Get coverage vocabularies entries
        JsonObject json = target().path("/vocabulary/coverage").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, vocabulary1Token)
                .get(JsonObject.class);
        Assert.assertEquals(249, json.getJsonArray("entries").size());
        JsonObject entry = json.getJsonArray("entries").getJsonObject(0);
        Assert.assertEquals("coverage-afg", entry.getString("id"));
        Assert.assertEquals("coverage", entry.getString("name"));
        Assert.assertEquals("Afghanistan", entry.getString("value"));
        Assert.assertEquals(0, entry.getJsonNumber("order").intValue());
        entry = json.getJsonArray("entries").getJsonObject(248);
        Assert.assertEquals("coverage-zwe", entry.getString("id"));
        Assert.assertEquals("coverage", entry.getString("name"));
        Assert.assertEquals("Zimbabwe", entry.getString("value"));
        Assert.assertEquals(248, entry.getJsonNumber("order").intValue());
        
        // Create a vocabulary entry with admin
        json = target().path("/vocabulary").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .put(Entity.form(new Form()
                        .param("name", "test-voc-1")
                        .param("value", "First value")
                        .param("order", "0")), JsonObject.class);
        String vocabulary1Id = json.getString("id");
        Assert.assertNotNull(vocabulary1Id);
        Assert.assertEquals("test-voc-1", json.getString("name"));
        Assert.assertEquals("First value", json.getString("value"));
        Assert.assertEquals(0, json.getJsonNumber("order").intValue());
        
        // Create a vocabulary entry with admin
        Response response = target().path("/vocabulary").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .put(Entity.form(new Form()
                        .param("name", "NOT_VALID")
                        .param("value", "First value")
                        .param("order", "0")));
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        // Get test-voc-1 vocabularies entries
        json = target().path("/vocabulary/test-voc-1").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, vocabulary1Token)
                .get(JsonObject.class);
        Assert.assertEquals(1, json.getJsonArray("entries").size());
        entry = json.getJsonArray("entries").getJsonObject(0);
        Assert.assertEquals(vocabulary1Id, entry.getString("id"));
        Assert.assertEquals("First value", entry.getString("value"));
        Assert.assertEquals(0, entry.getJsonNumber("order").intValue());
        
        // Update a vocabulary entry with admin
        json = target().path("/vocabulary/" + vocabulary1Id).request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .post(Entity.form(new Form()
                        .param("name", "test-voc-1-updated")
                        .param("value", "First value updated")
                        .param("order", "1")), JsonObject.class);
        Assert.assertEquals(vocabulary1Id, json.getString("id"));
        Assert.assertEquals("test-voc-1-updated", json.getString("name"));
        Assert.assertEquals("First value updated", json.getString("value"));
        Assert.assertEquals(1, json.getJsonNumber("order").intValue());
        
        // Get test-voc-1-updated vocabularies entries
        json = target().path("/vocabulary/test-voc-1-updated").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, vocabulary1Token)
                .get(JsonObject.class);
        Assert.assertEquals(1, json.getJsonArray("entries").size());
        entry = json.getJsonArray("entries").getJsonObject(0);
        Assert.assertEquals(vocabulary1Id, entry.getString("id"));
        Assert.assertEquals("First value updated", entry.getString("value"));
        Assert.assertEquals(1, entry.getJsonNumber("order").intValue());
        
        // Delete a vocabulary entry with admin
        target().path("/vocabulary/" + vocabulary1Id).request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .delete(JsonObject.class);
        
        // Get test-voc-1-updated vocabularies entries
        json = target().path("/vocabulary/test-voc-1-updated").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, vocabulary1Token)
                .get(JsonObject.class);
        Assert.assertEquals(0, json.getJsonArray("entries").size());
    }

    @Test(expected = NotFoundException.class)
    public void testNullResourceUpdateThrowsException() {  
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);

        // Update a non-existant vocabulary entry with admin
        target().path("/vocabulary/null").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .post(Entity.form(new Form()
                        .param("name", "-")
                        .param("value", "-")
                        .param("order", "1")), JsonObject.class);
    }

    @Test
    public void testNullParameterUpdateMakesNoChange() {
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);

        // Create a vocabulary entry with admin
        JsonObject json = target().path("/vocabulary").request()
        .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
        .put(Entity.form(new Form()
                .param("name", "test-voc-2")
                .param("value", "Second value")
                .param("order", "1")), JsonObject.class);
        String vocabulary1Id = json.getString("id");
        // Update a vocabulary entry with null parameters using admin
        json = target().path("/vocabulary/" + vocabulary1Id).request()
        .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
        .post(Entity.form(new Form()
                .param("name", null)
                .param("value", null)
                .param("order", null)), JsonObject.class);
        Assert.assertEquals(vocabulary1Id, json.getString("id"));
        // Expect no change
        Assert.assertEquals("test-voc-2", json.getString("name"));
        Assert.assertEquals("Second value", json.getString("value"));
        Assert.assertEquals(1, json.getJsonNumber("order").intValue());
    }

    @Test(expected = NotFoundException.class)
    public void testNullResourceDeleteThrowsException() {  
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);

        // Delete a non-existant vocabulary entry with admin
        target().path("/vocabulary/null").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .delete(JsonObject.class);
    }

    @Test(expected = ForbiddenException.class)
    public void testUnauthenticatedUserGetThrowsException() {
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);
        // Logout to invalidate token
        clientUtil.logout(adminToken);

        // Get vocabularies entries
        target().path("/vocabulary/test").request()
        .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
        .get(JsonObject.class);
    }

    @Test(expected = ForbiddenException.class)
    public void testUnauthenticatedUserAddThrowsException() {
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);
        // Logout to invalidate token
        clientUtil.logout(adminToken);

        // Create a vocabulary entry
        target().path("/vocabulary").request()
                .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
                .put(Entity.form(new Form()
                        .param("name", "unauthenticated")
                        .param("value", "value")
                        .param("order", "3")), JsonObject.class);
    }

    @Test(expected = ForbiddenException.class)
    public void testUnauthenticatedUserUpdateThrowsException() {
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);
        // Logout to invalidate token
        clientUtil.logout(adminToken);

        // Update a vocabulary entry 
        target().path("/vocabulary/test-voc-1").request()
        .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
        .post(Entity.form(new Form()
                .param("name", "test-voc-1-updated")
                .param("value", "First value updated")
                .param("order", "1")), JsonObject.class);
    }

    @Test(expected = ForbiddenException.class)
    public void testUnauthenticatedUserDeleteThrowsException() {
        // Login admin
        String adminToken = clientUtil.login("admin", "admin", false);
        // Logout to invalidate token
        clientUtil.logout(adminToken);

        // Delete a vocabulary entry 
        target().path("/vocabulary/test-voc-1-updated").request()
        .cookie(TokenBasedSecurityFilter.COOKIE_NAME, adminToken)
        .delete(JsonObject.class);
    }
}