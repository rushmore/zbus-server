package diskq


import "testing"

func TestMappedFile_GetExt(t *testing.T) {
	m, err := NewMappedFile("/tmp/MyTopic.idx", 1024)
	if err != nil {
		t.Fail()
	}
	defer m.Close()

	m.SetExt(0, "MyExtension")
	value, _ := m.GetExt(0)
	if value != "MyExtension" {
		t.Fail()
	}
}
