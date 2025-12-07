using _SPS.Models;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Firebase.Database;
using Firebase.Database.Query;
using System.Xml.Linq;

namespace _SPS.ViewModels
{
    // [QueryProperty]는 메인 화면에서 보낸 "Pet"이라는 택배를 받겠다는 뜻입니다.
    [QueryProperty(nameof(Pet), "Pet")]
    public partial class PetDetailViewModel : ObservableObject
    {
        // 받은 동물 데이터를 임시로 저장할 곳
        [ObservableProperty]
        private PetModel pet;

        // 화면에 보여줄 수정 가능한 데이터들
        [ObservableProperty] private string name;
        [ObservableProperty] private string species;
        [ObservableProperty] private string age;
        [ObservableProperty] private string description;

        private readonly FirebaseClient _dbClient;

        public PetDetailViewModel()
        {
            _dbClient = new FirebaseClient(Constants.FirebaseDatabaseUrl);
        }

        // 메인 화면에서 데이터(Pet)가 넘어왔을 때 자동으로 실행되는 함수입니다.
        // 넘어온 데이터를 입력창(Entry)들에 채워 넣습니다.
        partial void OnPetChanged(PetModel value)
        {
            if (value != null)
            {
                Name = value.Name;
                Species = value.Species;
                Age = value.Age;
                Description = value.Description;
            }
        }

        // [수정 기능]
        [RelayCommand]
        private async Task UpdatePet()
        {
            if (Pet == null) return;

            bool confirm = await Application.Current.MainPage.DisplayAlert("수정", "정보를 수정하시겠습니까?", "예", "아니요");
            if (!confirm) return;

            try
            {
                // 수정된 내용으로 객체 업데이트
                Pet.Name = Name;
                Pet.Species = Species;
                Pet.Age = Age;
                Pet.Description = Description;

                // Firebase의 해당 Key 위치에 덮어씌우기 (PutAsync)
                await _dbClient
                    .Child("Pets")
                    .Child(Pet.Key)
                    .PutAsync(Pet);

                await Application.Current.MainPage.DisplayAlert("성공", "수정되었습니다.", "확인");
                await Shell.Current.GoToAsync(".."); // 뒤로 가기
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("오류", "수정 실패: " + ex.Message, "확인");
            }
        }

        // [삭제 기능]
        [RelayCommand]
        private async Task DeletePet()
        {
            if (Pet == null) return;

            bool confirm = await Application.Current.MainPage.DisplayAlert("삭제", "정말 삭제하시겠습니까? 되돌릴 수 없습니다.", "삭제", "취소");
            if (!confirm) return;

            try
            {
                // Firebase에서 해당 Key 삭제
                await _dbClient
                    .Child("Pets")
                    .Child(Pet.Key)
                    .DeleteAsync();

                await Application.Current.MainPage.DisplayAlert("삭제됨", "삭제되었습니다.", "확인");
                await Shell.Current.GoToAsync(".."); // 뒤로 가기
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("오류", "삭제 실패: " + ex.Message, "확인");
            }
        }
    }
}