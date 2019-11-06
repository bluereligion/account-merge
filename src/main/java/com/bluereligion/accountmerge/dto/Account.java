package com.bluereligion.accountmerge.dto;

import java.util.Objects;

/**
 * Account class
 *
 * The account represents a user account and status information.
 *
 */
public class Account {

    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    private String accountName;
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    private String firstName;
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    private String createdOn;
    public String getCreatedOn() { return createdOn; }
    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private String statusSetOn;
    public String getStatusSetOn() { return statusSetOn; }
    public void setStatusSetOn(String statusSetOn) { this.statusSetOn = statusSetOn; }

    /**
     *  Used to store processing issue and error messages
     */
    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", accountName='" + accountName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", createdOn='" + createdOn + '\'' +
                ", status='" + status + '\'' +
                ", statusSetOn='" + statusSetOn + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) &&
                Objects.equals(accountName, account.accountName) &&
                Objects.equals(firstName, account.firstName) &&
                Objects.equals(createdOn, account.createdOn) &&
                Objects.equals(status, account.status) &&
                Objects.equals(statusSetOn, account.statusSetOn) &&
                Objects.equals(message, account.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountName, firstName, createdOn, status, statusSetOn, message);
    }

    private Account(AccountBuilder accountBuilder) {
        this.id = accountBuilder.id;
        this.accountName = accountBuilder.accountName;
        this.firstName = accountBuilder.firstName;
        this.createdOn = accountBuilder.createdOn;
        this.status = accountBuilder.status;
        this.statusSetOn = accountBuilder.statusSetOn;
        this.message = accountBuilder.message;
    }

    public static class AccountBuilder {

        private Long id;
        private String accountName;
        private String firstName;
        private String createdOn;
        private String status;
        private String statusSetOn;
        private String message;

        public AccountBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AccountBuilder accountName(String accountName) {
            this.accountName = accountName;
            return this;
        }

        public AccountBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public AccountBuilder createdOn(String createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public AccountBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AccountBuilder statusSetOn(String statusSetOn) {
            this.statusSetOn = statusSetOn;
            return this;
        }

        public AccountBuilder message(String message) {
            this.message = message;
            return this;
        }

        public Account build() {
            return new Account(this);
        }

    }

}
