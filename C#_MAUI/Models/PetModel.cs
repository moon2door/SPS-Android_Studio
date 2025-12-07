namespace _SPS.Models
{
    public class PetModel
    {
        // Firebase가 만들어주는 고유 ID (Key)
        public string Key { get; set; }

        // 동물 이름
        public string Name { get; set; }

        // 종 (개, 고양이 등)
        public string Species { get; set; }

        // 나이
        public string Age { get; set; }

        // 특징/설명
        public string Description { get; set; }

        // 등록한 사람의 ID (누가 등록했는지 알기 위해)
        public string OwnerId { get; set; }

        public string ImageUrl { get; set; }
    }
}