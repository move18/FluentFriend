package com.example.fluentfriend;

import android.content.DialogInterface;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserProfilePage extends AppCompatActivity implements MultiSpinner.MultiSpinnerListener {

    private User currentUser;
    private List<String> speakLanguages = new ArrayList<>();
    private List<String> learnLanguages = new ArrayList<>();
    private TextView displayUserName;
    private CheckBox cityWalksCheckBox;
    private CheckBox museumCheckBox;
    private CheckBox barCheckBox;
    private CheckBox fikaCheckBox;
    private CheckBox maleCheckBox;
    private CheckBox femaleCheckBox;
    private CheckBox otherCheckBox;
    private EditText editTextBiography;
    private Button buttonSave;
    private MultiSpinner languagesToLearnSpinner;
    private MultiSpinner languagesISpeakSpinner;
    private boolean[] checkedItems;
    private FirebaseDatabase db = FirebaseDatabase.getInstance("https://fluent-friend-dad39-default-rtdb.firebaseio.com/");
    private DatabaseReference usersRef = db.getReference().child("users");
    private DatabaseReference activeUsersRef = db.getReference().child("activeusers");
    private FirebaseStorage storage = FirebaseStorage.getInstance("gs://fluent-friend-dad39.appspot.com");
    private ImageView userImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //loads the current user
        currentUser = UserManager.getCurrentUser();

        //displays the current user's name
        displayUserName = findViewById(R.id.displayUserNameTextView);
        displayUserName.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

        //loads the current user's biography
        editTextBiography = findViewById(R.id.bioEditText);
        buttonSave = findViewById(R.id.saveSettingsButton); //vad gör denna /G
        loadBiography();

        languagesToLearnSpinner = (MultiSpinner) findViewById(R.id.languagesToLearnSpinner);
        languagesISpeakSpinner = (MultiSpinner) findViewById(R.id.languagesISpeakSpinner);



        userImage = findViewById(R.id.imageView);

        String profileImagePath = "fluent-friend-dad39.appspot.com/userimages" + currentUser.getEmail() + ".jpg";
        // Get a reference to the Firebase Storage
        String defaultImage = "fluent-friend-dad39.appspot.com/userimages/_0e85e3bc_a57f_4623_92c3_f2c2c78b33eb.jpg";

        // Create a storage reference
        StorageReference storageRef = storage.getReference();

        // Create a reference to the image file
        StorageReference imageRef = storageRef.child(profileImagePath);


        Picasso.get().load(imageRef.getPath()).placeholder(R.drawable._95cb4738_0cd5_47de_abc7_091916a074d2).error(R.drawable._3d0d656a_5f2b_4a78_bb27_477367edb14d).into(userImage);
        //connects the checkboxes graphic to variables in this class so we can use them
        //connects graphic elements in the xml code so we can change it in java code
        cityWalksCheckBox = findViewById(R.id.cityWalksCheckBox);
        museumCheckBox = findViewById(R.id.museumCheckBox);
        barCheckBox = findViewById(R.id.barCheckBox);
        fikaCheckBox = findViewById(R.id.fikaCheckBox);
        otherCheckBox = findViewById(R.id.otherCheckBox);
        femaleCheckBox = findViewById(R.id.femaleCheckBox);
        maleCheckBox = findViewById(R.id.maleCheckBox);

        //load current checkbox status from the user object
        cityWalksCheckBox.setChecked(currentUser.isCityWalksChecked());
        museumCheckBox.setChecked(currentUser.isMuseumChecked());
        barCheckBox.setChecked(currentUser.isBarChecked());
        fikaCheckBox.setChecked(currentUser.isFikaChecked());

        //gets the user's gender and sets the correct box
        loadGenderBox();

        // Change this toString() if you want some other text like Language or something
        languagesISpeakSpinner.setItems(LanguageManager.AVAILABLE_LANGUAGES,
                UserManager.getCurrentUser().getLanguagesSpeak().toString() , this);
        languagesToLearnSpinner.setItems(LanguageManager.AVAILABLE_LANGUAGES,
                UserManager.getCurrentUser().getLanguagesToLearn().toString(), this);
        loadUserLanguages();

        otherCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maleCheckBox.setChecked(false);
                femaleCheckBox.setChecked(false);
                otherCheckBox.setChecked(isChecked);
            }
        });

        femaleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                otherCheckBox.setChecked(false);
                maleCheckBox.setChecked(false);
                femaleCheckBox.setChecked(isChecked);
            }
        });

        maleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                femaleCheckBox.setChecked(false);
                otherCheckBox.setChecked(false);
                maleCheckBox.setChecked(isChecked);
            }
        });

        //if user click, changes to opposite value based on the current boolean
        cityWalksCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentUser.setCityWalksChecked(isChecked);
            }
        });
        museumCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentUser.setMuseumChecked(isChecked);
            }
        });

        barCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentUser.setBarChecked(isChecked);
            }
        });

        fikaCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                currentUser.setFikaChecked(isChecked); //vad gör denna?
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                saveBiography();
                saveCheckBoxes();
                saveGender();
                saveSelectedLanguages();
                // uploads to database
                usersRef.child(UserManager.getCurrentUser().getEmail()).setValue(UserManager.getCurrentUser());
                Toast.makeText(UserProfilePage.this, "Saved Successfully", Toast.LENGTH_SHORT).show();
                finish(); // Går tillbaka till därifrån man kom
            }
        });

    }

    private void saveSelectedLanguages() {
        UserManager.getCurrentUser().setLanguagesSpeak(speakLanguages);
        UserManager.getCurrentUser().setLanguagesToLearn(learnLanguages);

    }

    private void loadBiography() {
        String biography = currentUser.getBiography();
        editTextBiography.setText(biography);
    }

    private void loadGenderBox() {
        if (currentUser.getGender().equals("Male")) {
            maleCheckBox.setChecked(true);
        } else if (currentUser.getGender().equals("Female")) {
            femaleCheckBox.setChecked(true);
        } else if (currentUser.getGender().equals("Other/Private")) {
            otherCheckBox.setChecked(true);
        }
    }

    private void saveBiography(){
        String newBiography = editTextBiography.getText().toString();
        currentUser.setUserBiography(newBiography);
    }

    private void saveCheckBoxes() {
        currentUser.setCityWalksChecked(cityWalksCheckBox.isChecked());
        currentUser.setMuseumChecked(museumCheckBox.isChecked());
        currentUser.setBarChecked(barCheckBox.isChecked());
        currentUser.setFikaChecked(fikaCheckBox.isChecked());
    }

    private void saveGender() {
        if (femaleCheckBox.isChecked()) {
            currentUser.setGender("Female");
        } else if (maleCheckBox.isChecked()){
            currentUser.setGender("Male");
        } else if (otherCheckBox.isChecked()) {
            currentUser.setGender("Other/Private");
        }
    }

    // this method loads the users languages from the current user.
    private void loadUserLanguages(){
        speakLanguages = UserManager.getCurrentUser().getLanguagesSpeak();
        learnLanguages = UserManager.getCurrentUser().getLanguagesToLearn();
        if (learnLanguages != null && speakLanguages != null) {
            languagesToLearnSpinner.setItemsSelected(learnLanguages, LanguageManager.AVAILABLE_LANGUAGES);
            languagesISpeakSpinner.setItemsSelected(speakLanguages,LanguageManager.AVAILABLE_LANGUAGES);
        }
    }


    // this is the MultiSpinner classes clickHandler
    @Override
    public void onItemsSelected(boolean[] selected, MultiSpinner spinner) {
        ArrayList<String> speakLanguages = new ArrayList<>();
        ArrayList<String> learnLanguages = new ArrayList<>();
        for (int i = 0; i < selected.length; i++){
            if (selected[i]){
                if (spinner.getId() == languagesISpeakSpinner.getId()) {
                    speakLanguages.add(LanguageManager.AVAILABLE_LANGUAGES.get(i));
                }else if (spinner.getId() == languagesToLearnSpinner.getId()) {
                    learnLanguages.add(LanguageManager.AVAILABLE_LANGUAGES.get(i));
                }
            }
        }
        if (spinner.getId() == languagesISpeakSpinner.getId()) {
            this.speakLanguages = speakLanguages;
            spinner.setItemsSelected(this.speakLanguages, LanguageManager.AVAILABLE_LANGUAGES);
        }else if (spinner.getId() == languagesToLearnSpinner.getId()) {
            this.learnLanguages = learnLanguages;
            spinner.setItemsSelected(this.learnLanguages, LanguageManager.AVAILABLE_LANGUAGES);
        }
    }
}