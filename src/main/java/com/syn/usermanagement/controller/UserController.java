package com.syn.usermanagement.controller;

import com.syn.usermanagement.entity.User;
import com.syn.usermanagement.entity.WeatherResponse;
import com.syn.usermanagement.service.MessageProducer;
import com.syn.usermanagement.service.UserService;
import com.syn.usermanagement.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WeatherService weatherService;
    private final MessageProducer messageProducer;

    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "3") int size,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<User> createUser( @RequestBody User user) {
        User createdUser = userService.createUser(user);
//        messageProducer.sendObject(createdUser, "creation");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
             @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload user photo
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().build();
        }

        User updatedUser = userService.uploadUserPhoto(id, file);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user photo
     */
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<User> deletePhoto(@PathVariable Long id) {
        User updatedUser = userService.deleteUserPhoto(id);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherResponse> getUserByWeatherAPI() {
        String  url = "http://api.weatherstack.com/current?access_key=f4ae8bcd7c6773390ff253ec1e5f6606&query=Delhi";
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<WeatherResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(""), WeatherResponse.class
            );
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle 4xx and 5xx errors
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(null); // or parse e.getResponseBodyAsString()
        } catch (Exception e) {
            // Handle other errors (network issues, etc.)
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}