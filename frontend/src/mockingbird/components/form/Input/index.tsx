import React from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import type { InputProps } from '@platform-ui/input';
import PlatformInput from '@platform-ui/input';

type Props = InputProps & {
  control: Control;
};

export default function Input(props: Props) {
  const { name, label, control, required = false, disabled = false } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
    defaultValue: '',
  });
  return (
    <PlatformInput
      {...props}
      {...field}
      label={label}
      required={required}
      disabled={disabled}
    />
  );
}
