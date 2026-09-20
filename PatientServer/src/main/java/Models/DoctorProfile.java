package Models;

public class DoctorProfile {
    private String firstName;
    private String lastName;
    private String idNumber;
    private int age;
    private String organization;
    private String email;
    private String role;

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getIdNumber() { return idNumber; }
    public int getAge() { return age; }
    public String getOrganization() { return organization; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setAge(int age) { this.age = age; }
    public void setOrganization(String organization) { this.organization = organization; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
}
