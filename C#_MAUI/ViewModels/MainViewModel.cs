using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Firebase.Database;
using Firebase.Auth;
using System.Collections.ObjectModel;
using _SPS.Models;
using System.Linq; // [필수] 리스트 필터링(Where) 기능을 위해 필요

namespace _SPS.ViewModels
{
    public partial class MainViewModel : ObservableObject
    {
        [ObservableProperty] private string userEmail;
        [ObservableProperty] private bool isBusy;

        // [추가됨] 검색어 (입력할 때마다 자동으로 Search() 함수가 실행됨)
        [ObservableProperty]
        [NotifyPropertyChangedFor(nameof(Pets))] // 값이 바뀌면 Pets 목록 갱신 신호를 보냄
        private string searchText;

        // 화면에 보여줄 목록
        public ObservableCollection<PetModel> Pets { get; } = new();

        // [추가됨] DB에서 가져온 '원본' 전체 목록 (백업용)
        private List<PetModel> _allPets = new();

        private readonly FirebaseClient _dbClient;

        public MainViewModel()
        {
            _dbClient = new FirebaseClient(Constants.FirebaseDatabaseUrl);
            UserEmail = "관리자";
        }

        // 검색어가 바뀔 때마다 실행되는 함수 (OnSearchTextChanged)
        partial void OnSearchTextChanged(string value)
        {
            // 검색어가 비어있으면 전체 목록 보여주기
            if (string.IsNullOrWhiteSpace(value))
            {
                UpdateList(_allPets);
            }
            else
            {
                // 이름이나 종(Species)에 검색어가 포함된 것만 필터링
                var filtered = _allPets.Where(p =>
                    (p.Name != null && p.Name.Contains(value, StringComparison.OrdinalIgnoreCase)) ||
                    (p.Species != null && p.Species.Contains(value, StringComparison.OrdinalIgnoreCase))
                ).ToList();

                UpdateList(filtered);
            }
        }

        // 화면 목록(Pets)을 갈아끼우는 헬퍼 함수
        private void UpdateList(List<PetModel> list)
        {
            Pets.Clear();
            foreach (var item in list)
            {
                Pets.Add(item);
            }
        }

        public async Task LoadPets()
        {
            if (IsBusy) return;
            IsBusy = true;

            try
            {
                var collection = await _dbClient
                    .Child("Pets")
                    .OnceAsync<PetModel>();

                _allPets.Clear(); // 원본 리스트 초기화

                foreach (var item in collection)
                {
                    var pet = item.Object;
                    pet.Key = item.Key;
                    _allPets.Add(pet); // 원본에 저장
                }

                // 처음엔 전체 목록 보여주기 (검색어 초기화)
                SearchText = string.Empty;
                UpdateList(_allPets);
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("오류", "로딩 실패: " + ex.Message, "확인");
            }
            finally
            {
                IsBusy = false;
            }
        }

        // ... (나머지 NavigateToAddPet, Logout, GoToDetail 등은 그대로 두세요)
        [RelayCommand]
        private async Task NavigateToAddPet() => await Shell.Current.GoToAsync(nameof(Views.AddPetPage));

        [RelayCommand]
        private async Task Logout()
        {
            if (await Application.Current.MainPage.DisplayAlert("로그아웃", "로그아웃 하시겠습니까?", "예", "아니요"))
                await Shell.Current.GoToAsync("///LoginPage");
        }

        [RelayCommand]
        private async Task GoToDetail(PetModel selectedPet)
        {
            if (selectedPet == null) return;
            var param = new Dictionary<string, object> { { "Pet", selectedPet } };
            await Shell.Current.GoToAsync(nameof(Views.PetDetailPage), param);
        }
    }
}