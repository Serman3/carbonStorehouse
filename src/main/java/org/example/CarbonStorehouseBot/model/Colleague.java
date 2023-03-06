package org.example.CarbonStorehouseBot.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "colleague")
public class Colleague implements Serializable {

    @Id
    private Long chatId;

    private String firstName;

    private String lastName;

    private String userName;

    private LocalDateTime status_time;


    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getStatus_time() {
        return status_time;
    }

    public void setStatus_time(LocalDateTime status_time) {
        this.status_time = status_time;
    }
    @Override
    public String toString() {
        return "Colleague{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", status_time=" + status_time +
                '}';
    }
}
