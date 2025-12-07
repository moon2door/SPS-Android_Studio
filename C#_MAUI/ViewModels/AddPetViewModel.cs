using _SPS.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Firebase.Auth;
using Firebase.Auth.Providers;
using Firebase.Database;
using Firebase.Database.Query;
using Firebase.Storage; // [필수] 스토리지 사용
using System.IO;
using System.Threading.Tasks;

namespace _SPS.ViewModels
{
    public partial class AddPetViewModel : ObservableObject
    {
        [ObservableProperty] private string name;
        [ObservableProperty] private string species;
        [ObservableProperty] private string age;
        [ObservableProperty] private string description;
        [ObservableProperty] private bool isBusy;

        // 화면에 보여줄 사진(미리보기용)
        [ObservableProperty]
        private ImageSource petImageSource;

        // 실제로 선택된 파일 데이터
        private FileResult _selectedImageFile;

        private readonly FirebaseClient _dbClient;
        private readonly FirebaseAuthClient _authClient;

        public AddPetViewModel()
        {
            _dbClient = new FirebaseClient(Constants.FirebaseDatabaseUrl);

            var config = new FirebaseAuthConfig
            {
                ApiKey = Constants.FirebaseApiKey,
                AuthDomain = Constants.AuthDomain,
                Providers = new FirebaseAuthProvider[] { new EmailProvider() }
            };
            _authClient = new FirebaseAuthClient(config);
        }

        // [기능 1] 갤러리에서 사진 선택하기
        [RelayCommand]
        private async Task PickImage()
        {
            try
            {
                // MAUI 기본 기능으로 사진 선택창 열기
                var result = await MediaPicker.Default.PickPhotoAsync();

                if (result != null)
                {
                    _selectedImageFile = result;

                    // 화면에 미리보기 띄우기 (스트림 방식)
                    var stream = await result.OpenReadAsync();
                    PetImageSource = ImageSource.FromStream(() => stream);
                }
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("오류", "사진 선택 실패: " + ex.Message, "확인");
            }
        }

        // [기능 2] 저장하기 (사진 업로드 포함)
        [RelayCommand]
        private async Task SavePet()
        {
            if (IsBusy) return;
            if (string.IsNullOrWhiteSpace(Name) || string.IsNullOrWhiteSpace(Species))
            {
                await Application.Current.MainPage.DisplayAlert("알림", "이름과 종은 필수입니다.", "확인");
                return;
            }

            IsBusy = true;

            try
            {
                if (_authClient.User == null)
                {
                    await Application.Current.MainPage.DisplayAlert("오류", "로그인 정보가 없습니다.", "확인");
                    return;
                }

                // 1. 사진이 있다면 먼저 업로드하고 URL 받기
                string imageUrl = ""; // 사진 없으면 빈 문자열

                if (_selectedImageFile != null)
                {
                    // 파일을 읽기 위한 스트림 열기
                    using var stream = await _selectedImageFile.OpenReadAsync();

                    // 파일 이름 랜덤 생성 (중복 방지)
                    var fileName = $"{Guid.NewGuid()}.png";

                    // Firebase Storage에 업로드!
                    var storageTask = new FirebaseStorage(Constants.FirebaseStorageBucket)
                        .Child("PetImages") // 폴더 이름
                        .Child(fileName)
                        .PutAsync(stream);

                    // 업로드가 완료되면 다운로드 가능한 URL을 줍니다.
                    imageUrl = await storageTask;
                }

                // 2. DB에 데이터 저장 (이미지 URL 포함)
                var newPet = new PetModel
                {
                    Name = Name,
                    Species = Species,
                    Age = Age,
                    Description = Description,
                    OwnerId = _authClient.User.Uid,
                    ImageUrl = imageUrl // [핵심] 여기에 주소가 들어갑니다.
                };

                await _dbClient.Child("Pets").PostAsync(newPet);

                await Application.Current.MainPage.DisplayAlert("성공", $"{Name} 등록 완료!", "확인");
                await Shell.Current.GoToAsync("..");
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("오류", $"저장 실패: {ex.Message}", "확인");
            }
            finally
            {
                IsBusy = false;
            }
        }
    }
}