package Models.Patients;

public class Patient {

    private int id;
    private String givenName;
    private String familyName;
    private String gender;
    private int age;
    private String bloodPressure;

    private boolean sticky = false;

    // Creates an empty patient instance.
    public Patient() {}

    // Creates a patient with identifying details and basic clinical information.
    public Patient(int id, String givenName, String familyName,
                   String gender, int age,
                   String bloodPressure) {
        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
        this.gender = gender;
        this.age = age;
        this.bloodPressure = bloodPressure;
    }

    // Returns the unique patient identifier.
    public int getId() { return id; }
    // Returns the patient's full name.
    public String getName() { return givenName + " " + familyName; }
    // Returns the patient's first name.
    public String getGivenName() { return givenName; }
    // Returns the patient's last name.
    public String getFamilyName() { return familyName; }
    // Returns the patient's recorded gender.
    public String getGender() { return gender; }
    // Returns the patient's age.
    public int getAge() { return age; }
    // Returns the patient's blood pressure reading.
    public String getBloodPressure() { return bloodPressure; }
    // Returns whether the patient is marked as sticky in the UI.
    public boolean isSticky() { return sticky; }

    // Sets the patient's unique identifier.
    public void setId(int id) { this.id = id; }
    // Sets the patient's first name.
    public void setGivenName(String givenName) { this.givenName = givenName; }
    // Sets the patient's last name.
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    // Sets the patient's gender.
    public void setGender(String gender) { this.gender = gender; }
    // Sets the patient's age.
    public void setAge(int age) { this.age = age; }
    // Sets the patient's blood pressure reading.
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    // Marks or unmarks the patient as sticky.
    public void setSticky(boolean sticky) { this.sticky = sticky; }

    // Returns a human-readable summary of the patient's details.
    @Override
    public String toString() {
        return getName() + " (Gender: " + gender + ", Age: " + age + ", BP: " + bloodPressure + ")";
    }
}