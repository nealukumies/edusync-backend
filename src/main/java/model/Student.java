package model;

public class Student {
    private int id;
    private String name;
    private String email;
    private String role;

    public Student(int id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    @Override
    public String toString() {
        return "Student [id=" + id + ", name=" + name + ", email=" + email + ", role=" + role + "]";
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getRole() {
        return role;
    }
    public void setName(String name) {this.name=name;}
    public void setEmail(String email) {this.email=email;}
}
