package com.pratishaad.homelibrarymanagement.addbooks;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.DirectAction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.pratishaad.homelibrarymanagement.Book;
import com.pratishaad.homelibrarymanagement.MainActivity;
import com.pratishaad.homelibrarymanagement.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddBook extends AppCompatActivity {

    EditText title,author,ifbn,desc;
    ImageView coverimg,titleimg,authimg,ifbnimg,descimg;
    Spinner genre;
    Button addbook, addanotherbook, clearbtn;
    RadioButton radioButton;
    RadioGroup radioGroup;
    ProgressBar progressBar;
    final int REQUEST_IMAGE_CAPTURE = 1;



    FirebaseAuth fAuth;

    //DatabaseReference
    StorageReference storageRef;
    DatabaseReference databaseRef;

    byte bb[];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar_layout);
        View view =getSupportActionBar().getCustomView();

        progressBar=(ProgressBar) findViewById(R.id.AddBookProgressBar);

        title=(EditText)findViewById(R.id.title);
        author=(EditText)findViewById(R.id.author);
        ifbn=(EditText)findViewById(R.id.ifbn);
        desc=(EditText)findViewById(R.id.desc);

        coverimg=(ImageView)findViewById(R.id.coverimg);
        titleimg=(ImageView)findViewById(R.id.titleimg);
        authimg=(ImageView)findViewById(R.id.authimg);
        ifbnimg=(ImageView)findViewById(R.id.ifbnimg);
        descimg=(ImageView)findViewById(R.id.descimg);

        genre=(Spinner)findViewById(R.id.genre);

        radioGroup=(RadioGroup)findViewById(R.id.radio_group_currentlyReading);

        addbook=(Button)findViewById(R.id.addbook);
        clearbtn=(Button)findViewById(R.id.clear);


        //authenticated user
        fAuth = FirebaseAuth.getInstance();


        //getting user ID under which books are to be added
        storageRef = FirebaseStorage.getInstance().getReference();
        databaseRef= FirebaseDatabase.getInstance().getReference();


        //check app level permission given for camera
        if(checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},101);
        }

        //Clear Button
        clearbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                title.setText("");
                author.setText("");
                ifbn.setText("");
                desc.setText("");
            }
        });

        //Add Book to Firebase Database
        addbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                uploadBookToFirebase(bb);
            }
        });
    }


    //IMAGE TO TEXT CODE
    public void captureImage(View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if((view.toString()).equals(findViewById(R.id.coverimg).toString())) startActivityForResult(intent, 105);
        else if((view.toString()).equals(findViewById(R.id.titleimg).toString())) startActivityForResult(intent, 101);
        else if((view.toString()).equals(findViewById(R.id.authimg).toString())) startActivityForResult(intent, 102);
        else if((view.toString()).equals(findViewById(R.id.ifbnimg).toString())) startActivityForResult(intent, 103);
        else if((view.toString()).equals(findViewById(R.id.descimg).toString())) startActivityForResult(intent, 104);


    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();
        //extract image from bundle
        Bitmap bitmap = (Bitmap) bundle.get("data");

        //set image to image view
        if(requestCode==105) {
            onCaptureImageResult(data);
            coverimg.setImageBitmap(bitmap);
        }
        else if(requestCode==101) titleimg.setImageBitmap(bitmap);
        else if(requestCode==102) authimg.setImageBitmap(bitmap);
        else if(requestCode==103) ifbnimg.setImageBitmap(bitmap);
        else if(requestCode==104) descimg.setImageBitmap(bitmap);
        //process the image to extract text
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        Task<Text> result =
                recognizer
                        .process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                if(requestCode==101) title.setText(visionText.getText());
                                else if(requestCode==102) author.setText(visionText.getText());
                                else if(requestCode==103) ifbn.setText(visionText.getText());
                                else if(requestCode==104) desc.setText(visionText.getText());

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(),"Error!",Toast.LENGTH_SHORT).show();
                                    }
                                });
    }
    //Convert captured title image to bit map
    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        bb = bytes.toByteArray();
    }


    //upload book to firebase
    protected void uploadBookToFirebase(byte[] bb) {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "JPEG_" + timeStamp + "_";
        final String imagePath = "myimages/"+imageFileName+".jpg";
        final StorageReference imgRef = storageRef.child(imagePath);


        imgRef.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //Toast.makeText(getApplicationContext(),"Upload successful",Toast.LENGTH_SHORT).show();
                imgRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Uri downloadURI = uri;

                        String mTitle=title.getText().toString().trim();
                        String mAuthor=author.getText().toString().trim();
                        String mISBN=ifbn.getText().toString().trim();
                        String mDescription=desc.getText().toString().trim();
                        String mGenre=genre.getSelectedItem().toString();
                        String mImageFirebaseURI="";
                        String mCurrentlyReading="";

                        int selectedId = radioGroup.getCheckedRadioButtonId();
                        radioButton = (RadioButton) findViewById(selectedId);
                        mCurrentlyReading=radioButton.getText().toString();
                        if(mCurrentlyReading.equals(null)) mCurrentlyReading="No";

                        String mlendBookBool="No";
                        String mLendLendeeName="N/A";
                        String mLendGiveDate="N/A";
                        String mLendReceiveDate="N/A";


                        if(!(TextUtils.isEmpty(mTitle)) ){
                            String uid=fAuth.getUid();
                            String books="AllBooks";
                            String bookID = databaseRef.child(uid).push().getKey();
                            mImageFirebaseURI=downloadURI.toString();
                            Book book = new Book(bookID,mTitle,mAuthor,mISBN,mDescription,mGenre,mImageFirebaseURI,
                                    mCurrentlyReading,mlendBookBool,mLendLendeeName,mLendGiveDate,mLendReceiveDate);
                            databaseRef.child(uid).child(books).child(bookID).setValue(book);
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(),"This Book has been added ",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(),"Book Failed to ADD",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @org.jetbrains.annotations.NotNull Exception e) {
                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onOptionsItemSelected(View view) {
        try {
            Intent intent = new Intent (Intent.ACTION_VIEW , Uri.parse("mailto:" + "vyas.contact.in@gmail.com"));
            startActivity(intent);
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), "Sorry...You don't have any mail app", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}