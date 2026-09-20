package Models;

public class DoctorProfile {
    private String firstName;
    private String lastName;
    private String idNumber;
    private int age;
    private String orgnization;
    private String email;

    // Creates an empty doctor profile.
    public DoctorProfile() {}

    // Creates a doctor profile with identifying and professional details.
    public DoctorProfile(String firstName, String lastName, String idNumber, int age, String orgnization, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.idNumber = idNumber;
        this.age = age;
        this.orgnization = orgnization;
        this.email = email;
    }

    // Returns a default doctor profile used for initial application setup.
    public static DoctorProfile defaults() {
        return new DoctorProfile(
                "Raymond",
                "Chen",
                "DOC123456",
                20,
                "Cardiac Surgeon",
                "doctor@mail.com"
        );
    }
    // Returns the doctor's first name.
    public String getFirstName() { return firstName; }
    // Updates the doctor's first name.
    public void setFirstName(String firstName) { this.firstName = firstName; }

    // Returns the doctor's last name.
    public String getLastName() { return lastName; }
    // Updates the doctor's last name.
    public void setLastName(String lastName) { this.lastName = lastName; }

    // Returns the doctor's full name.
    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }

    // Returns the doctor's identification number.
    public String getIdNumber() { return idNumber; }
    // Updates the doctor's identification number.
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    // Returns the doctor's age.
    public int getAge() { return age; }
    // Updates the doctor's age.
    public void setAge(int age) { this.age = age; }

    // Returns the doctor's medical specialty.
    public String getOrgnization() { return orgnization; }
    // Updates the doctor's medical specialty.
    public void setOrgnization(String orgnization) { this.orgnization = orgnization; }

    // Returns the doctor's email address.
    public String getEmail() { return email; }
    // Updates the doctor's email address.
    public void setEmail(String email) { this.email = email; }
}
