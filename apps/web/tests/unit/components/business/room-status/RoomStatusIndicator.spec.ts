import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import { ElTag, ElIcon, ElButton } from 'element-plus';
import RoomStatusIndicator from '@/components/business/RoomStatusIndicator.vue';
import { useRoomStore } from '@/stores/roomStore';

// Mock the room store
vi.mock('@/stores/roomStore', () => ({
  useRoomStore: () => ({
    getStatusDisplayText: (status: string) => {
      const statusMap: Record<string, string> = {
        'AVAILABLE': '可用',
        'OCCUPIED': '已占用',
        'MAINTENANCE': '维护中',
        'CLEANING': '清洁中'
      };
      return statusMap[status] || status;
    },
    getStatusColor: (status: string) => {
      const colorMap: Record<string, string> = {
        'AVAILABLE': 'success',
        'OCCUPIED': 'error',
        'MAINTENANCE': 'warning',
        'CLEANING': 'info'
      };
      return colorMap[status] || 'default';
    }
  })
}));

describe('RoomStatusIndicator', () => {
  const mockRoom = {
    id: 1,
    status: 'AVAILABLE',
    version: 2,
    lastStatusChangedAt: '2023-12-07T10:30:00Z'
  };

  it('renders correctly with room prop', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const statusTag = wrapper.find('.status-tag');
    expect(statusTag.exists()).toBe(true);
    expect(statusTag.text()).toContain('可用');
  });

  it('renders correctly with status prop', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        status: 'OCCUPIED'
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const statusTag = wrapper.find('.status-tag');
    expect(statusTag.exists()).toBe(true);
    expect(statusTag.text()).toContain('已占用');
  });

  it('shows version when showVersion is true', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom,
        showVersion: true
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const versionInfo = wrapper.find('.version-info');
    expect(versionInfo.exists()).toBe(true);
    expect(versionInfo.text()).toBe('v2');
  });

  it('shows last updated time when showLastUpdated is true', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom,
        showLastUpdated: true
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const lastUpdated = wrapper.find('.last-updated');
    expect(lastUpdated.exists()).toBe(true);
    expect(lastUpdated.text()).toBeTruthy();
  });

  it('shows history button when showHistoryButton is true', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom,
        showHistoryButton: true
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const historyButton = wrapper.find('.el-button');
    expect(historyButton.exists()).toBe(true);
    expect(historyButton.text()).toContain('历史记录');
  });

  it('emits show-history event when history button is clicked', async () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom,
        showHistoryButton: true
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const historyButton = wrapper.find('.el-button');
    await historyButton.trigger('click');

    expect(wrapper.emitted('show-history')).toBeTruthy();
    expect(wrapper.emitted('show-history')).toHaveLength(1);
  });

  it('uses correct status colors', () => {
    const testCases = [
      { status: 'AVAILABLE', expectedColor: 'success' },
      { status: 'OCCUPIED', expectedColor: 'error' },
      { status: 'MAINTENANCE', expectedColor: 'warning' },
      { status: 'CLEANING', expectedColor: 'info' }
    ];

    testCases.forEach(({ status, expectedColor }) => {
      const wrapper = mount(RoomStatusIndicator, {
        props: {
          status
        },
        global: {
          components: {
            ElTag,
            ElIcon,
            ElButton
          }
        }
      });

      const statusTag = wrapper.findComponent(ElTag);
      expect(statusTag.props('type')).toBe(expectedColor);
    });
  });

  it('handles unknown status gracefully', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        status: 'UNKNOWN'
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const statusTag = wrapper.find('.status-tag');
    expect(statusTag.text()).toContain('UNKNOWN');
  });

  it('applies correct size classes', () => {
    const sizes = ['large', 'default', 'small'];
    sizes.forEach(size => {
      const wrapper = mount(RoomStatusIndicator, {
        props: {
          status: 'AVAILABLE',
          size: size as any
        },
        global: {
          components: {
            ElTag,
            ElIcon,
            ElButton
          }
        }
      });

      const statusTag = wrapper.findComponent(ElTag);
      expect(statusTag.props('size')).toBe(size);
    });
  });

  it('applies correct effect classes', () => {
    const effects = ['dark', 'light', 'plain'];
    effects.forEach(effect => {
      const wrapper = mount(RoomStatusIndicator, {
        props: {
          status: 'AVAILABLE',
          effect: effect as any
        },
        global: {
          components: {
            ElTag,
            ElIcon,
            ElButton
          }
        }
      });

      const statusTag = wrapper.findComponent(ElTag);
      expect(statusTag.props('effect')).toBe(effect);
    });
  });

  it('hides icon when showIcon is false', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        status: 'AVAILABLE',
        showIcon: false
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const icon = wrapper.find('.status-icon');
    expect(icon.exists()).toBe(false);
  });

  it('prioritizes status prop over room.status', () => {
    const wrapper = mount(RoomStatusIndicator, {
      props: {
        room: mockRoom,
        status: 'MAINTENANCE'
      },
      global: {
        components: {
          ElTag,
          ElIcon,
          ElButton
        }
      }
    });

    const statusTag = wrapper.find('.status-tag');
    expect(statusTag.text()).toContain('维护中');
    expect(statusTag.text()).not.toContain('可用');
  });
});