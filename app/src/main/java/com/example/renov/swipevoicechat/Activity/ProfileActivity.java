package com.example.renov.swipevoicechat.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.RequestOptions;
import com.example.renov.swipevoicechat.Model.User;
import com.example.renov.swipevoicechat.Network.NetRetrofit;
import com.example.renov.swipevoicechat.Network.network.ProgressHandler;
import com.example.renov.swipevoicechat.Network.network.RequestManager;
import com.example.renov.swipevoicechat.Network.network.VolleyMultipartRequest;
import com.example.renov.swipevoicechat.R;
import com.example.renov.swipevoicechat.Util.ImageUtil;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import jp.wasabeef.glide.transformations.BlurTransformation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();
    @BindView(R.id.iv_crop)
    ImageView ivCrop;
    @BindView(R.id.btn_complete)
    TextView btnComplete;
    @BindView(R.id.btn_profile_register)
    ImageButton btnProfileRegister;

    Unbinder unbinder;
    User myinfo;
    Uri mCropImageUri;

    private int CAMERA_CODE = 1;
    private int GALLERY_CODE = 2;

    MultiTransformation multiTransformation = new MultiTransformation(new BlurTransformation(25, 1),
            new CircleCrop(),
            new FitCenter());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        unbinder = ButterKnife.bind(this);

        Glide.with(getApplicationContext())
                .load(R.drawable.button_profile)
                .apply(RequestOptions.bitmapTransform(multiTransformation))
                .into(ivCrop);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @OnClick(R.id.iv_back)
    public void onClickBack() {
        finish();
    }

    @OnClick(R.id.btn_complete)
    public void onClickComplete() {
//        TODO: 프로필 사진 업로드
        if (mCropImageUri == null) {
            Toast.makeText(this, "프로필 사진을 등록해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        ImageUpload(mCropImageUri);

    }

    private void ImageUpload(Uri mCropImageUri) {
//        File imageFile = new File(mCropImageUri.getPath());

        File imageFile = new File(ImageUtil.getFilePathFromUri(mCropImageUri, getApplicationContext()));
//        int fileSize = Integer.parseInt(String.valueOf(imageFile.length()/1024));
        int fileSize = (int) imageFile.length();
        Call<Map> call = NetRetrofit.getInstance(this).getService().getUploadMetaData("image", fileSize);
        call.enqueue(new Callback<Map>() {
            @Override
            public void onResponse(Call<Map> call, Response<Map> response) {
                try {
                    Log.d(TAG, "response raw: " + response.raw());
                    Log.d(TAG, "response headers: " + response.headers());
                    Log.d(TAG, "response body: " + response.body());

                    if (response.errorBody() != null)
                        Log.d(TAG, "response error body: " + response.errorBody().string());


                    if (response.isSuccessful()) {
                        String key = (String) response.body().get("key");
                        String Host = (String) response.body().get("Host");

                        Log.d(TAG, "key: " + key);
                        Log.d(TAG, "Host: " + Host);
                        
                        String fileName = key.replace("image/", "");
                        uploadImage(response.body(), imageFile);

//                        fileName = fileName.replace(".jpg", "");


//                        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, RequestBody.create(MediaType.parse("image/*"), imageFile));
//                        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, RequestBody.create(MediaType.parse("image/*"), imageFile));
//                        response.body().put("file", imageFile);

//                        Call<String> request = FileUploadRetrofit.getInstance(getApplicationContext()).getService().upload(key, response.body(), filePart);
//                        request.enqueue(new Callback<String>() {
//                            @Override
//                            public void onResponse(Call<String> call, Response<String> response) {
//                                if(response.isSuccessful()){
//                                    Log.d(TAG,"body: " + response.body());
//                                } else {
//                                    try {
//                                        Log.d(TAG, "error code: " + response.code() + " error body: " + response.errorBody().string());
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//
//                            @Override
//                            public void onFailure(Call<String> call, Throwable t) {
//
//                            }
//                        });
                    } else {
                        Log.e(TAG, "error code: " + response.code() + " error body: " + response.errorBody().string());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Map> call, Throwable t) {

            }
        });
    }

    public void uploadImage(Map updateInfo, File imagefile) {
        final String uploadImagePath = "https://" + updateInfo.get("Host") + "/" + updateInfo.get("key");
        Log.d(TAG, uploadImagePath);
        
        String filePath = ImageUtil.getFilePathFromUri(getTempUri(), getApplicationContext());
        if (filePath == null || "".equals(filePath)) {
            return;
        }

        final ProgressHandler progressHandler = new ProgressHandler(this, false);

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest("https://hellovoicebucket.s3.amazonaws.com",
                response -> {
                    Log.d(TAG,"onResponse : " + response);

                    progressHandler.onCancel();

                    ImageUtil.deleteImageByUri(getTempUri(), getApplicationContext());
                    updateUserInfo(uploadImagePath);
                    Toast.makeText(this, "image upload Success", Toast.LENGTH_SHORT).show();
//                        리워드 보상 팝업 띄우기
//                        OneButtonAlertDialogFragment oneButtonAlertDialogFragment = OneButtonAlertDialogFragment.newInstance();
//                        Bundle bundle = new Bundle();
//                        bundle.putString("title", "매력 카드 평가를 통해 1버찌를 얻었습니다.");
//                        oneButtonAlertDialogFragment.setArguments(bundle);
//                        oneButtonAlertDialogFragment.show
//                        oneButtonAlertDialogFragment.show(getFragmentManager(), "oneButtonAlertDialogFragment");

//                        아직 프로필 승인이 안되었고, 보이스 등록이 안되었을 경우 경우에
//                        String userStatus = user.getStatus();
//                        int order = profileImageViewList.indexOf(selectedProfileImageView);
//
//                        LogUtil.d("user.getStatus(): " + user.getStatus());
//                        String status = SharedPrefHelper.getInstance(ProfileActivity.this).getSharedPreferences(SharedPrefHelper.USER_STATUS,null);
//                        LogUtil.d("sharedpref: " + status);
//
//
//                        if(order == 1 &&
//                                "P".equals(userStatus) &&
//                                (user.getVoice() == null | "".equals(user.getVoice()))){
//                            OneButtonAlertDialogFragment oneButtonAlertDialogFragment = OneButtonAlertDialogFragment.newInstance();
//                            Bundle bundle = new Bundle();
//                            bundle.putString(AlertDialogFragment.DIALOG_TITLE_NAME, "프로필 보이스 이벤트에 참여하세요!!");
//                            bundle.putString(AlertDialogFragment.DIALOG_DESCRIPTION_NAME, getString(R.string.profile_voice_reward));
//                            bundle.putString(AlertDialogFragment.DIALOG_CONFIRM_NAME, "확인");
//                            oneButtonAlertDialogFragment.setArguments(bundle);
//                            oneButtonAlertDialogFragment.show(getSupportFragmentManager(), "oneButtonAlertDialogFragment");
//                        }
                    moveToMain();
                }, error -> {
                    Log.d(TAG,"onErrorResponse : " + error.getMessage());

                    //TODO : 프로그레스 스타트
                    ImageUtil.deleteImageByUri(getTempUri(), getApplicationContext());
                    Toast.makeText(ProfileActivity.this, "error", Toast.LENGTH_SHORT).show();
                });

        updateInfo.remove("Host");
        updateInfo.remove("uploadImagePath");
        multipartRequest.addStringParams(updateInfo);
        multipartRequest.addAttachment(VolleyMultipartRequest.MEDIA_TYPE_JPEG, "file", imagefile);
        multipartRequest.buildRequest();
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

//        multipartRequest.setFixedStreamingMode(true);

        //TODO : 프로그레스 종료
        RequestManager.addRequest(multipartRequest, "ProfileMultipart");
        
        
    }

    private void moveToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void updateUserInfo(String uploadImagePath) {
//        getMyInfo(uploadImagePath);
        User myInfo = (User) getIntent().getParcelableExtra("user");

        Log.e(TAG,"user birth: " + myInfo.getBirth());
        myInfo.setProfileImageUrl(uploadImagePath);

        Call<User> request = NetRetrofit.getInstance(this).getService().updateUserInfo(myInfo);
        request.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.isSuccessful()){
                    moveToMain();
                } else {
                    try {
                        Log.e(TAG, "code: " + response.code() + " message: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {

            }
        });
    }

    public void getMyInfo(String uploadImagePath) {
        Call<User> request =  NetRetrofit.getInstance(this).getService().checkCurrentUserInfo();
        request.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(response.isSuccessful()){
                    myinfo = response.body();

                    Log.d(TAG,"gender: " + myinfo.getGender() +
                            "\nlat: " + myinfo.getLat() +
                            "\nlng: " + myinfo.getLng() +
                            "\nprofileImageUrl: " + myinfo.getProfileImageUrl() +
                            "\nbirth: " + myinfo.getBirth());

                    Log.e(TAG, "profileImageUrl: " + (myinfo.getProfileImageUrl() == null ? "null" : "exist" ));

                    myinfo.setProfileImageUrl(uploadImagePath);

                } else {
                    try {
                        Log.e(TAG,"raw: " + response.raw());
                        Log.e(TAG,"code: " + response.code());
                        Log.e(TAG,"headers: " + response.headers());
                        Log.e(TAG,"error body: " + response.errorBody().string());
                        Toast.makeText(ProfileActivity.this, "code: " + response.code() + "error message: " + response.errorBody().string(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {

            }
        });
    }

    private Uri getTempUri() {
        Uri uri = null;
        try {
            uri = Uri.fromFile(ImageUtil.createTempImageFileForProfile());
        } catch (Exception e) {
            Log.w(TAG,"getTempUri fail : " + e.getMessage());
        }
        return uri;
    }

    @SuppressLint("NewApi")
    @OnClick(R.id.iv_crop)
    public void onClickCrop() {
        if (CropImage.isExplicitCameraPermissionRequired(this)) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE);
            return;
        }

        //        else
//            new MaterialDialog.Builder(ProfileActivity.this)
//                    .title("프로필 등록하기")
//                    .items(R.array.profile)
//                    .itemsCallbackSingleChoice(0, (dialog, itemView, which, text) -> {
//                        switch (which) {
//                            case 0:
//                                selectCamera();
//                                break;
//                            case 1:
//                                selectGallery();
//                                break;
//                        }
//                        return true;
//                    })
//                    .positiveText("확인")
//                    .negativeText("취소")
//                    .show();


//        권한 요청
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropResultSize(200, 300)
                .setFixAspectRatio(true)
                .setAspectRatio(3, 4)
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .start(this);
    }

    @OnClick(R.id.btn_profile_register)
    public void onClickButtonProfileRegister() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropResultSize(200, 300)
                .setFixAspectRatio(true)
                .setAspectRatio(3, 4)
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .start(this);
    }

    private void selectCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        Uri outputFileUri = Uri.fromFile(new File(this.getExternalCacheDir().getPath(), "pickImageResult.jpeg"));
        File newFile = new File(getApplicationContext().getExternalCacheDir(), "pickImageResult.jpeg");

        Uri outputFileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", newFile);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, CAMERA_CODE);
    }

    private void selectGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_CODE);
        CropImage.startPickImageActivity(this);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle result of pick image chooser
        if (requestCode == CAMERA_CODE && resultCode == Activity.RESULT_OK || requestCode == GALLERY_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);
            Log.d(TAG, "image uri: " + imageUri.toString());
            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
            } else {
                // no permissions required or already granted, can start crop image activity
                startCropImageActivity(imageUri);
            }
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
//                Uri resultUri = mCropImageUri;
                mCropImageUri = resultUri;
//                ivCrop.setImageURI(resultUri);
                Glide.with(getApplicationContext())
                        .load(mCropImageUri)
                        .apply(RequestOptions.bitmapTransform(multiTransformation))
                        .into(ivCrop);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectGallery();
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
            if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // required permissions granted, start crop image activity
                selectCamera();
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropResultSize(200, 300)
                .setFixAspectRatio(true)
                .setAspectRatio(3, 4)
                .setAllowFlipping(false)
                .setAllowRotation(false)
                .start(this);
    }
}
