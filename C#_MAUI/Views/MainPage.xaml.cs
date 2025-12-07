using _SPS.ViewModels;

namespace _SPS.Views;

public partial class MainPage : ContentPage
{
    public MainPage()
    {
        InitializeComponent();
    }

    // 화면이 나타날 때마다 실행되는 함수 (안드로이드의 onResume과 비슷)
    protected override async void OnAppearing()
    {
        base.OnAppearing();

        // 뷰모델에 있는 LoadPets() 함수를 실행시켜라!
        if (BindingContext is MainViewModel viewModel)
        {
            await viewModel.LoadPets();
        }
    }
}