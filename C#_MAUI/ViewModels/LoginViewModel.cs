using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Firebase.Auth;
using Firebase.Auth.Providers;
using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;
using _SPS.Views;

namespace _SPS.ViewModels
{
    public partial class LoginViewModel : ObservableObject
    {
        [ObservableProperty]
        private string email;

        [ObservableProperty]
        private string password;

        [ObservableProperty]
        [NotifyCanExecuteChangedFor(nameof(LoginCommand))]
        [NotifyCanExecuteChangedFor(nameof(NavigateToRegisterCommand))]
        private bool isBusy;

        public bool CanExecute => !IsBusy;

        private readonly FirebaseAuthClient _authClient;

        public LoginViewModel()
        {
            var config = new FirebaseAuthConfig
            {
                ApiKey = Constants.FirebaseApiKey,
                AuthDomain = Constants.AuthDomain,
                Providers = new FirebaseAuthProvider[]
                {
                    new EmailProvider() 
                }
            };

            _authClient = new FirebaseAuthClient(config);
        }

        [RelayCommand(CanExecute = nameof(CanExecute))]
        private async Task Login()
        {
            if (string.IsNullOrWhiteSpace(Email) || string.IsNullOrWhiteSpace(Password))
            {
                await Application.Current.MainPage.DisplayAlert("오류", "이메일과 비밀번호를 입력해주세요.", "확인");
                return;
            }

            IsBusy = true;

            try
            {
                var userCredential = await _authClient.SignInWithEmailAndPasswordAsync(Email, Password);

                var user = userCredential.User;

                await Application.Current.MainPage.DisplayAlert("성공", $"{user.Info.Email}님 환영합니다!", "시작");

                await Shell.Current.GoToAsync("///MainTabs");
            }
            catch (Exception ex)
            {
                await Application.Current.MainPage.DisplayAlert("로그인 실패", "이메일 또는 비밀번호를 확인해주세요.", "확인");
            }
            finally
            {
                IsBusy = false;
            }
        }

        [RelayCommand(CanExecute = nameof(CanExecute))]
        private async Task NavigateToRegister()
        {
            // RegisterPage로 이동!
            await Shell.Current.GoToAsync(nameof(RegisterPage));
        }
    }
}